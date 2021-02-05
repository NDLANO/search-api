/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.util.concurrent.Executors
import com.sksamuel.elastic4s.http.ElasticDsl.{simpleStringQuery, _}
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.sksamuel.elastic4s.searches.suggestion.{DirectGenerator, PhraseSuggestion}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.{
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  SearchIndexes
}
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api.ResultWindowTooLargeException
import no.ndla.searchapi.model.domain.{Language, RequestInfo, SearchResult}
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.search.settings.SearchSettings

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait MultiSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with ArticleIndexService
    with LearningPathIndexService =>
  val multiSearchService: MultiSearchService

  class MultiSearchService extends LazyLogging with SearchService with TaxonomyFiltering {
    override val searchIndex = List(SearchIndexes(SearchType.Articles), SearchIndexes(SearchType.LearningPaths))
    override val indexServices = List(articleIndexService, learningPathIndexService)

    def matchingQuery(settings: SearchSettings): Try[SearchResult] = {
      val contentSearch = settings.query.map(queryString => {

        val langQueryFunc = (fieldName: String, boost: Int) =>
          buildSimpleStringQueryForField(
            queryString,
            fieldName,
            boost,
            settings.language,
            settings.fallback,
            searchDecompounded = true
        )

        boolQuery().must(
          boolQuery().should(
            langQueryFunc("title", 6),
            langQueryFunc("introduction", 2),
            langQueryFunc("metaDescription", 1),
            langQueryFunc("content", 1),
            langQueryFunc("tags", 1),
            langQueryFunc("embedAttributes", 1),
            simpleStringQuery(queryString).field("authors", 1),
            simpleStringQuery(queryString).field("grepContexts.title", 1),
            simpleStringQuery(queryString).field("embedResources", 1),
            simpleStringQuery(queryString).field("embedIds", 1),
            idsQuery(queryString)
          ))
      })

      val embedResourceSearch = settings.embedResource.map(q => {
        boolQuery()
          .should(
            simpleStringQuery(q).field("embedResources", 1),
          )
      })

      val embedIdSearch = settings.embedId.map(q => {
        boolQuery()
          .should(
            simpleStringQuery(q).field("embedIds", 1),
          )
      })

      val boolQueries: List[BoolQuery] = List(contentSearch, embedResourceSearch, embedIdSearch).flatten
      val fullQuery = boolQuery().must(boolQueries)

      executeSearch(settings, fullQuery)
    }

    def executeSearch(settings: SearchSettings, baseQuery: BoolQuery): Try[SearchResult] = {
      val filteredSearch = baseQuery.filter(getSearchFilters(settings))

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {

        val aggregations = buildTermsAggregation(settings.aggregatePaths)

        val searchToExecute = search(searchIndex)
          .query(filteredSearch)
          .suggestions(suggestions(settings.query, settings.language, settings.fallback))
          .from(startAt)
          .size(numResults)
          .highlighting(highlight("*"))
          .aggs(aggregations)
          .sortBy(getSortDefinition(settings.sort, settings.language))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt != 0) { searchToExecute } else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                totalCount = response.result.totalHits,
                page = Some(settings.page),
                pageSize = numResults,
                language = if (settings.language == "*") Language.AllLanguages else settings.language,
                results = getHits(response.result, settings.language, settings.fallback),
                suggestions = getSuggestions(response.result),
                aggregations = getAggregationsFromResult(response.result),
                scrollId = response.result.scrollId
              ))
          case Failure(ex) => Failure(ex)
        }
      }
    }

    /**
      * Returns a list of QueryDefinitions of different search filters depending on settings.
      *
      * @param settings SearchSettings object.
      * @return List of QueryDefinitions.
      */
    private def getSearchFilters(settings: SearchSettings): List[Query] = {
      val languageFilter = settings.language match {
        case "" | Language.AllLanguages =>
          None
        case lang =>
          if (settings.fallback) None else Some(existsQuery(s"title.$lang"))
      }

      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val licenseFilter = settings.license match {
        case None      => Some(boolQuery().not(termQuery("license", "copyrighted")))
        case Some(lic) => Some(termQuery("license", lic))
      }

      val grepCodesFilter =
        if (settings.grepCodes.nonEmpty)
          Some(termsQuery("grepContexts.code", settings.grepCodes))
        else None

      val taxonomyContextTypeFilter = contextTypeFilter(settings.learningResourceTypes)
      val taxonomyFilterFilter = levelFilter(settings.taxonomyFilters)
      val taxonomyResourceTypesFilter = resourceTypeFilter(settings.resourceTypes, settings.filterByNoResourceType)
      val taxonomySubjectFilter = subjectFilter(settings.subjects)
      val taxonomyRelevanceFilter = relevanceFilter(settings.relevanceIds, settings.subjects, settings.taxonomyFilters)

      val supportedLanguageFilter =
        if (settings.supportedLanguages.isEmpty) None
        else
          Some(
            boolQuery().should(
              settings.supportedLanguages.map(l => termQuery("supportedLanguages", l))
            )
          )

      List(
        licenseFilter,
        idFilter,
        languageFilter,
        taxonomyFilterFilter,
        taxonomySubjectFilter,
        taxonomyResourceTypesFilter,
        taxonomyContextTypeFilter,
        supportedLanguageFilter,
        taxonomyRelevanceFilter,
        grepCodesFilter
      ).flatten
    }

    override def scheduleIndexDocuments(): Unit = {
      val threadPoolSize = if (searchIndex.nonEmpty) searchIndex.size else 1
      implicit val ec: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))
      val requestInfo = RequestInfo()

      val articleFuture = Future {
        requestInfo.setRequestInfo()
        articleIndexService.indexDocuments()
      }
      val learningPathFuture = Future {
        requestInfo.setRequestInfo()
        learningPathIndexService.indexDocuments()
      }

      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.Articles), articleFuture)
      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.LearningPaths), learningPathFuture)
    }
  }

}
