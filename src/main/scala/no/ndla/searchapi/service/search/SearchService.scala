/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.lang.Math.max

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.SearchApiProperties.MaxPageSize
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with IndexService with SearchConverterService with LazyLogging =>

  trait SearchService[T] {
    val searchIndex: List[String]

    /**
      * Returns hit as summary
      * @param hit as json string
      * @param language language as ISO639 code
      * @return api-model summary of hit
      */
    def hitToApiModel(hit: SearchHit, language: String): T

    def getHits(response: SearchResponse, language: String, fallback: Boolean): Seq[T] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }
            hitToApiModel(result, matchedLanguage)
          })
        case _ => Seq.empty
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage => Language.DefaultLanguage
        case _                   => language
      }

      sort match {
        case (Sort.ByTitleAsc) =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _                           => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.ASC).missing("_last")
          }
        case (Sort.ByTitleDesc) =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _                           => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.DESC).missing("_last")
          }
        case (Sort.ByDurationAsc)     => fieldSort("duration").order(SortOrder.ASC).missing("_last")
        case (Sort.ByDurationDesc)    => fieldSort("duration").order(SortOrder.DESC).missing("_last")
        case (Sort.ByRelevanceAsc)    => fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc)   => fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByLastUpdatedAsc)  => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByIdAsc)           => fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc)          => fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
    }

    def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val numResults = max(pageSize.min(MaxPageSize), 0)
      val startAt = (page - 1).max(0) * numResults

      (startAt, numResults)
    }

    protected def scheduleIndexDocuments(): Unit

    /**
      * Takes care of logging reindexResults, used in subclasses overriding [[scheduleIndexDocuments]]
      * @param indexName
      * @param reindexFuture
      * @param executor Execution contex for the future
      */
    protected def handleScheduledIndexResults(indexName: String, reindexFuture: Future[Try[ReindexResult]])(
        implicit executor: ExecutionContext): Unit = {
      reindexFuture.onComplete {
        case Success(Success(reindexResult: ReindexResult)) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} $indexName in ${reindexResult.millisUsed} ms.")
        case Success(Failure(ex)) => logger.warn(ex.getMessage, ex)
        case Failure(ex)          => logger.warn(s"Unable to create index '$indexName': " + ex.getMessage, ex)
      }
    }

    protected def errorHandler[U](failure: Throwable): Failure[U] = {
      failure match {
        case e: NdlaSearchException =>
          e.rf.status match {
            case notFound: Int if notFound == 404 =>
              val msg = s"Index ${e.rf.error.index.getOrElse("")} not found. Scheduling a reindex."
              logger.error(msg)
              scheduleIndexDocuments()
              Failure(new IndexNotFoundException(msg))
            case _ =>
              logger.error(e.getMessage)
              Failure(
                new ElasticsearchException(s"Unable to execute search in ${e.rf.error.index.getOrElse("")}",
                                           e.getMessage))
          }
        case t: Throwable => Failure(t)
      }
    }

  }
}
