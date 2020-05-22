/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.lang.Math.max

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse, SuggestionResult}
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.{ElasticSearchScrollKeepAlive, MaxPageSize}
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api.{MultiSearchSuggestion, MultiSearchSummary, SearchSuggestion, SuggestOption}
import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.search.SearchType
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with IndexService with SearchConverterService with LazyLogging =>

  trait SearchService {
    val searchIndex: List[String]

    /**
      * Returns hit as summary
      *
      * @param hit as json string
      * @param language language as ISO639 code
      * @return api-model summary of hit
      */
    def hitToApiModel(hit: SearchHit, language: String): MultiSearchSummary = {
      val articleType = SearchApiProperties.SearchDocuments(SearchType.Articles)
      val draftType = SearchApiProperties.SearchDocuments(SearchType.Drafts)
      val learningPathType = SearchApiProperties.SearchDocuments(SearchType.LearningPaths)
      hit.`type` match {
        case `articleType` =>
          searchConverterService.articleHitAsMultiSummary(hit.sourceAsString, language)
        case `draftType` =>
          searchConverterService.draftHitAsMultiSummary(hit.sourceAsString, language)
        case `learningPathType` =>
          searchConverterService.learningpathHitAsMultiSummary(hit.sourceAsString, language)
      }
    }

    def getHits(response: SearchResponse, language: String, fallback: Boolean): Seq[MultiSearchSummary] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

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

    def getSuggestions(response: SearchResponse): Seq[MultiSearchSuggestion] = {
      response.suggestions.map {
        case (key, value) =>
          MultiSearchSuggestion(name = key, suggestions = getSuggestion(value))
      }.toSeq
    }

    def getSuggestion(results: Seq[SuggestionResult]): Seq[SearchSuggestion] = {
      results.map(
        result =>
          SearchSuggestion(text = result.text,
                           offset = result.offset,
                           length = result.length,
                           options = result.options.map(mapToSuggestOption)))
    }

    def mapToSuggestOption(optionsMap: Map[String, Any]): SuggestOption = {
      val text = optionsMap.getOrElse("text", "")
      val score = optionsMap.getOrElse("score", 1)
      SuggestOption(
        text.asInstanceOf[String],
        score.asInstanceOf[Double]
      )
    }

    def scroll(scrollId: String, language: String, fallback: Boolean): Try[SearchResult] = {
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .map(response => {
          val hits = getHits(response.result, language, fallback)
          val suggestions = getSuggestions(response.result)
          SearchResult(
            totalCount = response.result.totalHits,
            page = None,
            pageSize = response.result.hits.hits.length,
            language = if (language == "*") Language.AllLanguages else language,
            results = hits,
            suggestions = suggestions,
            scrollId = response.result.scrollId
          )
        })
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage => Language.DefaultLanguage
        case _                   => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _                           => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.ASC).missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _                           => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.DESC).missing("_last")
          }
        case Sort.ByDurationAsc     => fieldSort("duration").order(SortOrder.ASC).missing("_last")
        case Sort.ByDurationDesc    => fieldSort("duration").order(SortOrder.DESC).missing("_last")
        case Sort.ByRelevanceAsc    => fieldSort("_score").order(SortOrder.ASC)
        case Sort.ByRelevanceDesc   => fieldSort("_score").order(SortOrder.DESC)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").order(SortOrder.ASC).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").order(SortOrder.DESC).missing("_last")
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
      *
      * @param indexName Name of index to use for logging
      * @param reindexFuture Reindexing future to handle
      * @param executor Execution context for the future
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
