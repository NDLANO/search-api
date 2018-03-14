/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.alias.AliasActionDefinition
import com.typesafe.scalalogging.LazyLogging
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.ElasticIndexingException
import no.ndla.searchapi.model.domain.ReindexResult
import no.ndla.searchapi.model.domain.Language.languageAnalyzers
import no.ndla.searchapi.model.domain.article.Content
import no.ndla.searchapi.model.taxonomy.Bundle

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient
    with SearchApiClient
    with TaxonomyApiClient =>

  trait IndexService[D <: AnyRef, T <: AnyRef] extends LazyLogging {
    val apiClient: SearchApiClient
    val documentType: String
    val searchIndex: String

    def getMapping: MappingDefinition

    def createIndexRequest(domainModel: D, indexName: String, taxonomyBundle: Option[Bundle]): Try[IndexDefinition]

    def indexDocument(imported: D, taxonomyBundle: Option[Bundle] = None): Try[D] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        request <- createIndexRequest(imported, searchIndex, taxonomyBundle)
        _ <- e4sClient.execute {
          request
        }
      } yield imported
    }

    def indexDocuments(implicit mf: Manifest[D]): Try[ReindexResult] = synchronized {
      val start = System.currentTimeMillis()
      createIndexWithGeneratedName.flatMap(indexName => {
        val operations = for {
          numIndexed <- sendToElastic(indexName)
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

    def sendToElastic(indexName: String)(implicit mf: Manifest[D]): Try[Int] = {
      taxonomyApiClient.getTaxonomyBundle match {
        case Success(bundle) =>
          logger.info("Successfully fetched taxonomy...")
          val stream = apiClient.getChunks[D]
          var count = 0 // TODO: more functional? Is it even possible with streams?
          stream.foreach({
            case Success(c) =>
              indexDocuments(c, indexName, Some(bundle)).map(c => count += c)
            case Failure(ex) => return Failure(ex)
          })
          Success(count)
        case Failure(ex) =>
          logger.error("Could not fetch taxonomy for indexing...")
          Failure(ex)
      }
    }

    def indexDocuments(contents: Seq[D], indexName: String, taxonomyBundle: Option[Bundle]): Try[Int] = {
      if (contents.isEmpty) {
        Success(0)
      }
      else {
        val req = contents.map(content => createIndexRequest(content, indexName, taxonomyBundle))
        val indexRequests = req.collect{ case Success(indexRequest) => indexRequest }
        val failedToCreateRequests = req.collect{ case Failure(ex) => Failure(ex) }

        if(indexRequests.nonEmpty) {
            val response = e4sClient.execute {
              bulk(indexRequests)
            }

            response match {
              case Success(r) =>
                logger.info(s"Indexed ${contents.size} documents. No of failed items: ${r.result.failures.size + failedToCreateRequests.size}")
                Success(contents.size)
              case Failure(ex) => Failure(ex)
            }
        } else {
          logger.error(s"All ${contents.size} requests failed to be created.")
          Failure(ElasticIndexingException("No indexReqeusts were created successfully."))
        }
      }
    }

    def deleteDocument(contentId: Long): Try[_] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        deleted <- {
          e4sClient.execute {
            delete(s"$contentId").from(searchIndex / documentType)
          }
        }
      } yield deleted
    }

    def createIndexWithGeneratedName: Try[String] = createIndexWithName(searchIndex + "_" + getTimestamp)

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          createIndex(indexName)
            .mappings(getMapping)
            .indexSetting("max_result_window", SearchApiProperties.ElasticSearchIndexMaxResultWindow)
        }

        response match {
          case Success(_) => Success(indexName)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def getAliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute {
        getAliases(Nil, List(searchIndex))
      }

      response match {
        case Success(results) =>
          Success(results.result.mappings.headOption.map((t) => t._1.name))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val actions = oldIndexName match {
          case None =>
            List[AliasActionDefinition](addAlias(searchIndex).on(newIndexName))
          case Some(oldIndex) =>
            List[AliasActionDefinition](
              removeAlias(searchIndex).on(oldIndex),
              addAlias(searchIndex).on(newIndexName))
        }

        e4sClient.execute(aliases(actions)) match {
          case Success(_) =>
            logger.info("Alias target updated successfully, deleting other indexes.")
            cleanupIndexes()
          case Failure(ex) =>
            logger.error("Could not update alias target.")
            Failure(ex)
        }

      }
    }

    /**
      * Deletes every index that is not in use by this indexService.
      * Only indexes starting with indexName are deleted.
      * @param indexName Start of index names that is deleted if not aliased.
      * @return Name of aliasTarget.
      */
    def cleanupIndexes(indexName: String = searchIndex): Try[String] = {
      e4sClient.execute(getAliases()) match {
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

          if (toDelete.isEmpty){
            logger.info("No indexes to be deleted.")
            Success(aliasTarget)
          } else {
            e4sClient.execute {
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
            e4sClient.execute {
              deleteIndex(indexName)
            }
          }
      }

    }

    def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_) => Success(false)
        case Failure(ex) => Failure(ex)
      }
    }

    def getTimestamp: String = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)

    /**
      * Returns Sequence of FieldDefinitions for a given field.
      * @param fieldName Name of field in mapping.
      * @param keepRaw   Whether to add a keywordField named raw.
      *                  Usually used for sorting, aggregations or scripts.
      * @return Sequence of FieldDefinitions for a field.
      */
    protected def generateLanguageSupportedFieldList(fieldName: String, keepRaw: Boolean = false): Seq[FieldDefinition] = {
      keepRaw match {
        case true =>
          languageAnalyzers.map(langAnalyzer =>
            textField(s"$fieldName.${langAnalyzer.lang}")
              .fielddata(false)
              .analyzer(langAnalyzer.analyzer)
              .fields(keywordField("raw")))
        case false =>
          languageAnalyzers.map(langAnalyzer =>
            textField(s"$fieldName.${langAnalyzer.lang}")
              .fielddata(false)
              .analyzer(langAnalyzer.analyzer))
      }
    }
  }

}
