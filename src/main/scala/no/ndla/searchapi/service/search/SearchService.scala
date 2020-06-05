/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.lang.Math.max

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.sksamuel.elastic4s.requests.searches.suggestion.{DirectGenerator, PhraseSuggestion}
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchResponse, SuggestionResult}
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
    def hitToApiModel(hit: SearchHit, language: String): Option[MultiSearchSummary] = {
      val articleType = SearchApiProperties.SearchDocuments(SearchType.Articles)
      val draftType = SearchApiProperties.SearchDocuments(SearchType.Drafts)
      val learningPathType = SearchApiProperties.SearchDocuments(SearchType.LearningPaths)
      val hitType = hit.sourceAsMap.getOrElse("type", Some(hit.`type`))
      hitType match {
        case `articleType` =>
          Some(searchConverterService.articleHitAsMultiSummary(hit.sourceAsString, language))
        case `draftType` =>
          Some(searchConverterService.draftHitAsMultiSummary(hit.sourceAsString, language))
        case `learningPathType` =>
          Some(searchConverterService.learningpathHitAsMultiSummary(hit.sourceAsString, language))
        case _ => None
      }
    }

    def getHits(response: SearchResponse, language: String, fallback: Boolean): Seq[MultiSearchSummary] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.flatMap(result => {
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

    def suggestions(query: Option[String], language: String, fallback: Boolean): Seq[PhraseSuggestion] = {
      query
        .map(q => {
          val searchLanguage =
            if (language == Language.AllLanguages || fallback) "nb" else language
          Seq(
            suggestion(q, "title", searchLanguage) // TODO suggest on more fields?
          )
        })
        .getOrElse(Seq.empty)
    }

    def suggestion(query: String, field: String, language: String): PhraseSuggestion = {
      phraseSuggestion(name = field)
        .on(s"$field.$language.trigram")
        .addDirectGenerator(DirectGenerator(field = s"$field.$language.trigram", suggestMode = Some("always")))
        .size(1)
        .gramSize(3)
        .text(query)
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
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").sortOrder(SortOrder.Asc).missing("_last")
            case _                           => fieldSort(s"title.$sortLanguage.raw").sortOrder(SortOrder.Asc).missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").sortOrder(SortOrder.Desc).missing("_last")
            case _                           => fieldSort(s"title.$sortLanguage.raw").sortOrder(SortOrder.Desc).missing("_last")
          }
        case Sort.ByDurationAsc     => fieldSort("duration").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByDurationDesc    => fieldSort("duration").sortOrder(SortOrder.Desc).missing("_last")
        case Sort.ByRelevanceAsc    => fieldSort("_score").sortOrder(SortOrder.Asc)
        case Sort.ByRelevanceDesc   => fieldSort("_score").sortOrder(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").sortOrder(SortOrder.Desc).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").sortOrder(SortOrder.Desc).missing("_last")
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
