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
import com.sksamuel.elastic4s.searches.queries.{BoolQueryDefinition, QueryDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.mapping.ISO639
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.SearchIndexes
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api.{MultiSearchResult, MultiSearchSummary, ResultWindowTooLargeException}
import no.ndla.searchapi.model.domain.{Language, ReindexResult, RequestInfo}
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.search.{SearchSettings, SearchType}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait MultiSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with ArticleIndexService
    with LearningPathIndexService =>
  val multiSearchService: MultiSearchService

  class MultiSearchService extends LazyLogging with SearchService[MultiSearchSummary] {
    override val searchIndex = List(SearchIndexes(SearchType.Articles), SearchIndexes(SearchType.LearningPaths))

    override def hitToApiModel(hit: SearchHit, language: String): MultiSearchSummary = {
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

    def all(settings: SearchSettings): Try[MultiSearchResult] = executeSearch(settings, boolQuery())

    def matchingQuery(query: String, settings: SearchSettings): Try[MultiSearchResult] = {
      val searchLanguage =
        if (settings.language == Language.AllLanguages || settings.fallback) "*" else settings.language
      val titleSearch = simpleStringQuery(query).field(s"title.$searchLanguage", 2)
      val introSearch = simpleStringQuery(query).field(s"introduction.$searchLanguage", 2)
      val metaSearch = simpleStringQuery(query).field(s"metaDescription.$searchLanguage", 1)
      val contentSearch = simpleStringQuery(query).field(s"content.$searchLanguage", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$searchLanguage", 1)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              titleSearch,
              introSearch,
              metaSearch,
              contentSearch,
              tagSearch
            )
        )
      executeSearch(settings, fullQuery)
    }

    def executeSearch(settings: SearchSettings, baseQuery: BoolQueryDefinition): Try[MultiSearchResult] = {
      val filteredSearch = baseQuery.filter(getSearchFilters(settings))

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > SearchApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are ${SearchApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {
        val searchToExecute = search(searchIndex)
          .query(filteredSearch)
          .from(startAt)
          .size(numResults)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(settings.sort, settings.language))

        e4sClient.execute(searchToExecute) match {
          case Success(response) =>
            Success(
              MultiSearchResult(
                totalCount = response.result.totalHits,
                page = settings.page,
                pageSize = numResults,
                language = if (settings.language == "*") Language.AllLanguages else settings.language,
                results = getHits(response.result, settings.language, settings.fallback)
              ))
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    /**
      * Returns a list of QueryDefinitions of different search filters depending on settings.
      *
      * @param settings SearchSettings object.
      * @return List of QueryDefinitions.
      */
    private def getSearchFilters(settings: SearchSettings): List[QueryDefinition] = {
      val languageFilter = settings.language match {
        case "" | Language.AllLanguages =>
          None
        case lang =>
          settings.fallback match {
            case true  => None
            case false => Some(existsQuery(s"title.$lang"))
          }
      }

      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val licenseFilter = settings.license match {
        case None      => Some(boolQuery().not(termQuery("license", "copyrighted")))
        case Some(lic) => Some(termQuery("license", lic))
      }

      val taxonomyContextFilter = contextTypeFilter(settings.learningResourceTypes)
      val taxonomyFilterFilter = levelFilter(settings.taxonomyFilters)
      val taxonomyResourceTypesFilter = resourceTypeFilter(settings.resourceTypes)
      val taxonomySubjectFilter = subjectFilter(settings.subjects)

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
        taxonomyContextFilter,
        supportedLanguageFilter
      ).flatten
    }

    private def subjectFilter(subjects: List[String]) = {
      if (subjects.isEmpty) None
      else
        Some(
          boolQuery().should(
            subjects.map(
              subjectId =>
                nestedQuery("contexts").query(
                  termQuery(s"contexts.subjectId", subjectId)
              ))
          )
        )
    }

    private def levelFilter(taxonomyFilters: List[String]) = {
      if (taxonomyFilters.isEmpty) None
      else
        Some(
          boolQuery().should(
            taxonomyFilters.map(
              filterName =>
                nestedQuery("contexts.filters").query(
                  boolQuery().should(
                    ISO639.languagePriority.map(l => termQuery(s"contexts.filters.name.$l.raw", filterName))
                  )
              ))
          )
        )
    }

    private def resourceTypeFilter(resourceTypes: List[String]) = {
      if (resourceTypes.isEmpty) None
      else
        Some(
          boolQuery().should(
            resourceTypes.map(
              resourceTypeId =>
                nestedQuery("contexts").query(
                  termQuery(s"contexts.resourceTypeIds", resourceTypeId)
              ))
          )
        )
    }

    private def contextTypeFilter(contextTypes: List[LearningResourceType.Value]) = {
      if (contextTypes.isEmpty) None
      else
        Some(
          boolQuery().should(
            contextTypes.map(
              ct =>
                nestedQuery("contexts").query(
                  termQuery("contexts.contextType", ct.toString)
              ))
          )
        )
    }

    override def scheduleIndexDocuments(): Unit = {
      val threadPoolSize = if (searchIndex.nonEmpty) searchIndex.size else 1
      implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))
      val requestInfo = RequestInfo()

      val articleFuture = Future {
        requestInfo.setRequestInfo()
        articleIndexService.indexDocuments
      }
      val learningPathFuture = Future {
        requestInfo.setRequestInfo()
        learningPathIndexService.indexDocuments
      }

      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.Articles), articleFuture)
      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.LearningPaths), learningPathFuture)
    }
  }

}
