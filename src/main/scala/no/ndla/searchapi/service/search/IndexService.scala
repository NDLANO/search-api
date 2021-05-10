/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.Indexes

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.Executors
import com.sksamuel.elastic4s.alias.AliasAction
import com.sksamuel.elastic4s.analyzers.{
  CompoundWordTokenFilter,
  CustomAnalyzerDefinition,
  HyphenationDecompounder,
  LowercaseTokenFilter,
  ShingleTokenFilter,
  StandardTokenizer,
  WhitespaceTokenizer
}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition, NestedField}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.IndexBulkSize
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.ElasticIndexingException
import no.ndla.searchapi.model.domain.Language.languageAnalyzers
import no.ndla.searchapi.model.domain.{Content, Language, ReindexResult}
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle
import no.ndla.searchapi.repository.Repository
import scalikejdbc.{delete => scadelete, _}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}
import math.max
import scala.language.postfixOps

trait IndexService {
  this: Elastic4sClient with SearchApiClient with TaxonomyApiClient with GrepApiClient =>

  trait IndexService[D <: Content] extends LazyLogging {
    val apiClient: SearchApiClient
    val documentType: String
    val searchIndex: String
    val repository: Repository[D]

    val shingle: ShingleTokenFilter =
      ShingleTokenFilter(name = "shingle", minShingleSize = Some(2), maxShingleSize = Some(3))

    val trigram: CustomAnalyzerDefinition = CustomAnalyzerDefinition(name = "trigram",
                                                                     tokenizer = StandardTokenizer,
                                                                     filters = Seq(LowercaseTokenFilter, shingle))

    def getMapping: MappingDefinition

    def createIndexRequest(domainModel: D,
                           indexName: String,
                           taxonomyBundle: TaxonomyBundle,
                           grepBundle: GrepBundle): Try[IndexRequest]

    def indexDocument(imported: D): Try[D] = {
      val bundles = for {
        taxonomyBundle <- taxonomyApiClient.getTaxonomyBundle()
        grepBundle <- grepApiClient.getGrepBundle()
      } yield (taxonomyBundle, grepBundle)
      bundles match {
        case Failure(ex) =>
          logger.error(
            s"Grep and/or Taxonomy could not be fetched when indexing $documentType ${imported.id.map(id => s"with id: '$id'").getOrElse("")}")
          Failure(ex)
        case Success((taxonomyBundle, grepBundle)) => indexDocument(imported, taxonomyBundle, grepBundle)
      }
    }

    def indexDocument(imported: D, taxonomyBundle: TaxonomyBundle, grepBundle: GrepBundle): Try[D] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None        => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        request <- createIndexRequest(imported, searchIndex, taxonomyBundle, grepBundle)
        _ <- e4sClient.executeBlocking {
          request
        }
      } yield imported
    }

    def indexDocuments()(implicit mf: Manifest[D]): Try[ReindexResult] = {
      val bundles = for {
        taxonomyBundle <- taxonomyApiClient.getTaxonomyBundle()
        grepBundle <- grepApiClient.getGrepBundle()
      } yield (taxonomyBundle, grepBundle)
      bundles match {
        case Failure(ex) =>
          logger.error(s"Grep and/or Taxonomy could not be fetched when reindexing all $documentType")
          Failure(ex)
        case Success((taxonomyBundle, grepBundle)) => indexDocuments(taxonomyBundle, grepBundle)
      }
    }

    def indexDocuments(taxonomyBundle: TaxonomyBundle, grepBundle: GrepBundle)(
        implicit mf: Manifest[D]): Try[ReindexResult] = {
      val start = System.currentTimeMillis()
      createIndexWithGeneratedName.flatMap(indexName => {
        val operations = for {
          numIndexed <- sendToElastic(indexName, taxonomyBundle, grepBundle)
          aliasTarget <- getAliasTarget
          _ <- updateAliasTarget(aliasTarget, indexName)
        } yield numIndexed

        operations match {
          case Failure(f) =>
            deleteIndexWithName(Some(indexName))
            Failure(f)
          case Success(totalIndexed) =>
            Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
        }
      })
    }

    def sendToElastic(
        indexName: String,
        taxonomyBundle: TaxonomyBundle,
        grepBundle: GrepBundle
    )(implicit mf: Manifest[D]): Try[Int] = {
      implicit val executionContext: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(10))
      val pageSize = IndexBulkSize
      val numPages = repository.pageCount(pageSize)
      val pages = Seq.range(1, numPages + 1)

      val iterator = pages.iterator.map(p => {
        val (safePageNo, safePageSize) = (max(p, 1), max(pageSize, 0))
        repository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)
      })

      val indexed = iterator
        .map(chunk => {
          indexDocuments(
            chunk,
            indexName,
            taxonomyBundle,
            grepBundle
          ).map(numIndexed => numIndexed -> chunk.size)
        })
        .toList

      val chunks = Await.result(Future.sequence(indexed), Duration.Inf)
      executionContext.shutdown()

      chunks.collect { case (Failure(ex), chunkSize) => Failure(ex) } match {
        case Nil =>
          val successfulChunks = chunks.collect {
            case (Success(chunkIndexed), chunkSize) =>
              (chunkIndexed, chunkSize)
          }

          val (count, totalCount) = successfulChunks.foldLeft((0, 0)) {
            case ((totalIndexed, totalSize), (chunkIndexed, chunkSize)) =>
              (totalIndexed + chunkIndexed, totalSize + chunkSize)
          }

          logger.info(s"$count/$totalCount documents ($documentType) were indexed successfully.")
          Success(totalCount)
        case notEmpty => notEmpty.head

      }
    }

    def indexDocuments(
        contents: Seq[D],
        indexName: String,
        taxonomyBundle: TaxonomyBundle,
        grepBundle: GrepBundle
    )(implicit ec: ExecutionContext): Future[Try[Int]] = {
      if (contents.isEmpty) {
        Future.successful { Success(0) }
      } else {
        val req = contents.map(content => createIndexRequest(content, indexName, taxonomyBundle, grepBundle))
        val indexRequests = req.collect { case Success(indexRequest) => indexRequest }
        val failedToCreateRequests = req.collect { case Failure(ex)  => Failure(ex) }

        if (indexRequests.isEmpty && failedToCreateRequests.isEmpty) {
          logger.info(s"(Correctly) Created no index-requests for ${contents.size}.")
          Future.successful(Success(0))
        } else if (indexRequests.isEmpty) {
          logger.error(s"All ${contents.size} requests failed to be created.")
          Future.successful(Failure(ElasticIndexingException("No indexRequests were created successfully.")))
        } else {
          e4sClient
            .executeAsync(bulk(indexRequests))
            .map {
              case Failure(ex) =>
                logger.error(s"Failed to index ${contents.size} documents ($documentType): ${ex.getMessage}", ex)
                Failure(ex)
              case Success(r) =>
                val numFailed = r.result.failures.size + failedToCreateRequests.size
                logger.info(s"Indexed ${contents.size} documents ($documentType). No of failed items: $numFailed")
                Success(contents.size - numFailed)
            }
        }

      }
    }

    def deleteDocument(contentId: Long): Try[_] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None        => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        deleted <- {
          e4sClient.executeBlocking {
            delete(s"$contentId").from(searchIndex / documentType)
          }
        }
      } yield deleted
    }

    def createIndexWithGeneratedName: Try[String] = createIndexWithName(searchIndex + "_" + getTimestamp)

    private val customCompoundAnalyzer =
      CustomAnalyzerDefinition(
        "compound_analyzer",
        WhitespaceTokenizer,
        CompoundWordTokenFilter(
          name = "hyphenation_decompounder",
          `type` = HyphenationDecompounder,
          wordListPath = Some("compound-words-norwegian-wordlist.txt"),
          hyphenationPatternsPath = Some("hyph/no.xml"),
          minSubwordSize = Some(4),
          onlyLongestMatch = Some(false)
        )
      )

    private val customExactAnalyzer =
      CustomAnalyzerDefinition(
        "exact",
        WhitespaceTokenizer
      )

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.executeBlocking {
          createIndex(indexName)
            .mappings(getMapping)
            .analysis(
              trigram,
              Language.nynorskLanguageAnalyzer,
              customCompoundAnalyzer,
              customExactAnalyzer
            )
            .indexSetting("max_result_window", SearchApiProperties.ElasticSearchIndexMaxResultWindow)
            .replicas(0)
            .shards(1)
            .includeTypeName(true)
        }

        response match {
          case Success(_)  => Success(indexName)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def getAliasTarget: Try[Option[String]] = {
      val response = e4sClient.executeBlocking {
        getAliases(Nil, List(searchIndex))
      }

      response match {
        case Success(results) =>
          Success(results.result.mappings.headOption.map(t => t._1.name))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateReplicaNumber(indexName: String): Try[_] = {
      e4sClient.executeBlocking {
        updateSettings(Indexes(indexName), Map("number_of_replicas" -> "1"))
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = synchronized {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val actions = oldIndexName match {
          case None =>
            List[AliasAction](addAlias(searchIndex).on(newIndexName))
          case Some(oldIndex) =>
            List[AliasAction](removeAlias(searchIndex).on(oldIndex), addAlias(searchIndex).on(newIndexName))
        }

        e4sClient.executeBlocking(aliases(actions)) match {
          case Success(_) =>
            logger.info("Alias target updated successfully, deleting other indexes.")
            for {
              _ <- cleanupIndexes()
              _ <- updateReplicaNumber(newIndexName)
            } yield ()
          case Failure(ex) =>
            logger.error("Could not update alias target.")
            Failure(ex)
        }

      }
    }

    /**
      * Deletes every index that is not in use by this indexService.
      * Only indexes starting with indexName are deleted.
      *
      * @param indexName Start of index names that is deleted if not aliased.
      * @return Name of aliasTarget.
      */
    def cleanupIndexes(indexName: String = searchIndex): Try[String] = {
      e4sClient.executeBlocking(getAliases()) match {
        case Success(s) =>
          val indexes = s.result.mappings.filter(_._1.name.startsWith(indexName))
          val unreferencedIndexes = indexes.filter(_._2.isEmpty).map(_._1.name).toList
          val (aliasTarget, aliasIndexesToDelete) = indexes.filter(_._2.nonEmpty).map(_._1.name) match {
            case head :: tail =>
              (head, tail)
            case _ =>
              logger.warn("No alias found, when attempting to clean up indexes.")
              ("", List.empty)
          }

          val toDelete = unreferencedIndexes ++ aliasIndexesToDelete

          if (toDelete.isEmpty) {
            logger.info("No indexes to be deleted.")
            Success(aliasTarget)
          } else {
            e4sClient.executeBlocking {
              deleteIndex(toDelete)
            } match {
              case Success(_) =>
                logger.info(s"Successfully deleted unreferenced and redundant indexes.")
                Success(aliasTarget)
              case Failure(ex) =>
                logger.error("Could not delete unreferenced and redundant indexes.")
                Failure(ex)
            }
          }
        case Failure(ex) =>
          logger.warn("Could not fetch aliases after updating alias.")
          Failure(ex)
      }

    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) =>
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            e4sClient.executeBlocking {
              deleteIndex(indexName)
            }
          }
      }

    }

    def countDocuments: Long = {
      val response = e4sClient.executeBlocking {
        catCount(searchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.executeBlocking {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_)                          => Success(false)
        case Failure(ex)                         => Failure(ex)
      }
    }

    def getTimestamp: String = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)

    /**
      * Returns Sequence of FieldDefinitions for a given field.
      *
      * @param fieldName Name of field in mapping.
      * @param keepRaw   Whether to add a keywordField named raw.
      *                  Usually used for sorting, aggregations or scripts.
      * @return Sequence of FieldDefinitions for a field.
      */
    protected def generateLanguageSupportedFieldList(
        fieldName: String,
        keepRaw: Boolean = false
    ): List[FieldDefinition] = {
      languageAnalyzers.map(langAnalyzer => {
        val sf = List(
          textField("trigram").analyzer("trigram"),
          textField("decompounded")
            .searchAnalyzer("standard")
            .analyzer("compound_analyzer"),
          textField("exact")
            .analyzer("exact")
        )

        val subFields = if (keepRaw) sf :+ keywordField("raw") else sf

        textField(s"$fieldName.${langAnalyzer.lang}")
          .analyzer(langAnalyzer.analyzer)
          .fields(subFields)
      })
    }

    protected def getTaxonomyContextMapping: NestedField = {
      nestedField("contexts").fields(
        List(
          keywordField("id"),
          keywordField("path"),
          keywordField("contextType"),
          keywordField("subjectId"),
          keywordField("parentTopicIds")
        ) ++
          generateLanguageSupportedFieldList("subject", keepRaw = true) ++
          generateLanguageSupportedFieldList("breadcrumbs") ++
          List(
            nestedField("filters").fields(
              List(
                keywordField("filterId"),
                keywordField("relevanceId")
              ) ++
                generateLanguageSupportedFieldList("name", keepRaw = true) ++
                generateLanguageSupportedFieldList("relevance")
            )
          ) ++
          List(
            nestedField("resourceTypes").fields(
              List(keywordField("id")) ++
                generateLanguageSupportedFieldList("name", keepRaw = true)
            ))
      )
    }

  }

}
