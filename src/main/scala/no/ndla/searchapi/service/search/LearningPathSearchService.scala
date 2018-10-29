/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.util.concurrent.Executors

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.BoolQuery
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api
import no.ndla.searchapi.SearchApiProperties.SearchIndexes
import no.ndla.searchapi.model.api.ResultWindowTooLargeException
import no.ndla.searchapi.model.api.learningpath.LearningPathSummary
import no.ndla.searchapi.model.domain.{Language, RequestInfo, Sort}
import no.ndla.searchapi.model.search.SearchType

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait LearningPathSearchService {
  this: Elastic4sClient with SearchConverterService with SearchService with LearningPathIndexService =>
  val learningPathSearchService: LearningPathSearchService

  class LearningPathSearchService extends LazyLogging with SearchService[LearningPathSummary] {
    override val searchIndex = List(SearchIndexes(SearchType.LearningPaths))

    override def hitToApiModel(hit: SearchHit, language: String): LearningPathSummary = {
      searchConverterService.hitAsLearningPathSummary(hit.sourceAsString, language)
    }

    def all(withIdIn: List[Long],
            taggedWith: Option[String],
            language: String,
            sort: Sort.Value,
            page: Int,
            pageSize: Int,
            fallback: Boolean): Try[api.SearchResult[LearningPathSummary]] = {
      val searchLanguage = if (language == Language.AllLanguages || fallback) "*" else language
      val fullQuery = searchLanguage match {
        case "*" => boolQuery()
        case lang => {
          val titleSearch = existsQuery(s"title.$lang")
          val descSearch = existsQuery(s"description.$lang")

          boolQuery()
            .should(
              titleSearch,
              descSearch
            )
        }
      }

      executeSearch(
        fullQuery,
        withIdIn,
        taggedWith,
        sort,
        searchLanguage,
        page,
        pageSize,
        fallback
      )
    }

    def matchingQuery(query: String,
                      withIdIn: List[Long],
                      taggedWith: Option[String],
                      language: String,
                      sort: Sort.Value,
                      page: Int,
                      pageSize: Int,
                      fallback: Boolean): Try[api.SearchResult[LearningPathSummary]] = {
      val searchLanguage = if (language == Language.AllLanguages || fallback) "*" else language

      val titleSearch = simpleStringQuery(query).field(s"title.$searchLanguage", 2)
      val descSearch = simpleStringQuery(query).field(s"description.$searchLanguage", 2)
      val stepTitleSearch = simpleStringQuery(query).field(s"learningsteps.title.$searchLanguage", 1)
      val stepDescSearch = simpleStringQuery(query).field(s"learningsteps.description.$searchLanguage", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$searchLanguage", 2)
      val authorSearch = simpleStringQuery(query).field("author", 1)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              titleSearch,
              descSearch,
              nestedQuery("learningsteps", stepTitleSearch)
                .scoreMode(ScoreMode.Avg)
                .boost(1)
                .inner(innerHits("learningsteps.title")),
              nestedQuery("learningsteps", stepDescSearch)
                .scoreMode(ScoreMode.Avg)
                .boost(1)
                .inner(innerHits("learningsteps.description")),
              tagSearch,
              authorSearch
            )
        )

      executeSearch(fullQuery, withIdIn, taggedWith, sort, searchLanguage, page, pageSize, fallback)
    }

    def executeSearch(queryBuilder: BoolQuery,
                      withIdIn: List[Long],
                      taggedWith: Option[String],
                      sort: Sort.Value,
                      language: String,
                      page: Int,
                      pageSize: Int,
                      fallback: Boolean): Try[api.SearchResult[LearningPathSummary]] = {

      val tagFilter = taggedWith match {
        case None      => None
        case Some(tag) => Some(termQuery(s"tags.$language.raw", tag))
      }
      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val filters = List(tagFilter, idFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = page * numResults
      if (requestedResultWindow > SearchApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are ${SearchApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {
        val searchToExecute = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(sort, language))

        e4sClient.execute(searchToExecute) match {
          case Success(response) =>
            Success(
              api.SearchResult[LearningPathSummary](
                response.result.totalHits,
                page,
                numResults,
                if (language == "*") Language.AllLanguages else language,
                getHits(response.result, language, fallback)
              ))
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      val threadPoolSize = if (searchIndex.nonEmpty) searchIndex.size else 1
      implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))
      val requestInfo = RequestInfo()

      val learningPathFuture = Future {
        requestInfo.setRequestInfo()
        learningPathIndexService.indexDocuments
      }

      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.LearningPaths), learningPathFuture)
    }
  }

}
