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
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.mapping.ISO639
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.SearchApiProperties.SearchIndexes
import no.ndla.searchapi.model.api.{MultiSearchResult, MultiSearchSummary, ResultWindowTooLargeException}
import no.ndla.searchapi.model.domain.{Language, RequestInfo}
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait MultiDraftSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with IndexService
    with SearchService
    with DraftIndexService
    with LearningPathIndexService =>
  val multiDraftSearchService: MultiDraftSearchService

  class MultiDraftSearchService extends LazyLogging with SearchService[MultiSearchSummary] {

    override val searchIndex = List(SearchIndexes(SearchType.Drafts), SearchIndexes(SearchType.LearningPaths))

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

    def matchingQuery(settings: MultiDraftSearchSettings): Try[MultiSearchResult] = {
      val searchLanguage =
        if (settings.language == Language.AllLanguages || settings.fallback) "*" else settings.language

      val contentSearch = settings.query.map(q => {
        val titleSearch = simpleStringQuery(q).field(s"title.$searchLanguage", 2)
        val introSearch = simpleStringQuery(q).field(s"introduction.$searchLanguage", 2)
        val metaSearch = simpleStringQuery(q).field(s"metaDescription.$searchLanguage", 1)
        val contentSearch = simpleStringQuery(q).field(s"content.$searchLanguage", 1)
        val tagSearch = simpleStringQuery(q).field(s"tags.$searchLanguage", 1)
        val authorSearch = simpleStringQuery(q).field("authors", 1)

        boolQuery()
          .should(
            titleSearch,
            introSearch,
            metaSearch,
            contentSearch,
            tagSearch,
            authorSearch
          )
      })

      val noteSearch = settings.noteQuery.map(q => {
        boolQuery()
          .should(
            simpleStringQuery(q).field("notes", 1)
          )
      })

      val boolQueries: List[BoolQuery] = List(contentSearch, noteSearch).flatten
      val fullQuery = boolQuery().must(boolQueries)

      executeSearch(settings, fullQuery)
    }

    def executeSearch(settings: MultiDraftSearchSettings, baseQuery: BoolQuery): Try[MultiSearchResult] = {
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
    private def getSearchFilters(settings: MultiDraftSearchSettings): List[Query] = {
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
      val taxonomyTopicFilter = topicFilter(settings.topics)
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
        taxonomyTopicFilter,
        taxonomyResourceTypesFilter,
        taxonomyContextFilter,
        supportedLanguageFilter,
        taxonomyRelevanceFilter
      ).flatten
    }

    private def relevanceFilter(relevanceIds: List[String], subjectIds: List[String], levels: List[String]) =
      if (relevanceIds.isEmpty) None
      else
        Some(
          boolQuery().should(
            relevanceIds.map(
              relevanceId =>
                nestedQuery("contexts").query(
                  boolQuery().must(
                    nestedQuery("contexts.filters").query(boolQuery().must(
                      termQuery("contexts.filters.relevanceId", relevanceId),
                      boolQuery().should(levels.map(f =>
                        boolQuery().should(ISO639.languagePriority.map(l =>
                          termQuery(s"contexts.filters.name.$l.raw", f)))))
                    )),
                    boolQuery().should(subjectIds.map(sId => termQuery("contexts.subjectId", sId)))
                  )
              )
            )
          ))

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

    private def topicFilter(topics: List[String]) = {
      if (topics.isEmpty) None
      else
        Some(
          boolQuery().should(
            topics.map(
              topicId =>
                nestedQuery("contexts").query(
                  termQuery(s"contexts.parentTopicIds", topicId)
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
                nestedQuery("contexts.resourceTypes").query(
                  termQuery(s"contexts.resourceTypes.id", resourceTypeId)
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

      val draftFuture = Future {
        requestInfo.setRequestInfo()
        draftIndexService.indexDocuments()
      }
      val learningPathFuture = Future {
        requestInfo.setRequestInfo()
        learningPathIndexService.indexDocuments()
      }

      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.Drafts), draftFuture)
      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.LearningPaths), learningPathFuture)
    }
  }

}
