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
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.SearchDocuments
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.api.{MultiSearchSummary, ResultWindowTooLargeException, SearchResult}
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.domain.{Language, Sort}
import no.ndla.searchapi.model.search.SearchSettings

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait MultiSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with ArticleIndexService =>
  val multiSearchService: MultiSearchService

  class MultiSearchService extends LazyLogging with SearchService[MultiSearchSummary] {
    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))

    override val searchIndex: String = SearchApiProperties.SearchIndexes("articles")

    override def hitToApiModel(hit: SearchHit, language: String): MultiSearchSummary = {

      val articleType = SearchApiProperties.SearchDocuments("articles")
      val learningpathType = SearchApiProperties.SearchDocuments("learningpaths")
      hit.`type` match {
        case `articleType` =>
          searchConverterService.articleHitAsMultiSummary(hit.sourceAsString, language)
        case `learningpathType` =>
          searchConverterService.articleHitAsMultiSummary(hit.sourceAsString, language)
        //TODO: searchConverterService.learningpathHitAsMultiSummary(hit.sourceAsString, language)
      }

    }

    def all(settings: SearchSettings): Try[SearchResult[MultiSearchSummary]] = {
      executeSearch(settings, boolQuery())
    }

    def matchingQuery(query: String, settings: SearchSettings): Try[SearchResult[MultiSearchSummary]] = {
      val searchLanguage = if (settings.language == Language.AllLanguages || settings.fallback) "*" else settings.language
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

    def executeSearch(settings: SearchSettings, queryBuilder: BoolQueryDefinition): Try[api.SearchResult[MultiSearchSummary]] = {
      val typesFilter = if (settings.types.nonEmpty) Some(constantScoreQuery(termsQuery("articleType", settings.types))) else None
      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val licenseFilter = settings.license match {
        case None => Some(noCopyright)
        case Some(lic) => Some(termQuery("license", lic))
      }

      val (languageFilter, searchLanguage) = settings.language match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          settings.fallback match {
            case true => (None, "*")
            case false => (Some(existsQuery(s"title.$lang")), lang)
          }
      }

      val filters = List(licenseFilter, idFilter, languageFilter, typesFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > SearchApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${SearchApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {

        val searchToExec = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(settings.sort, searchLanguage))

        e4sClient.execute(searchToExec) match {
          case Success(response) =>
            Success(api.SearchResult[MultiSearchSummary](
              response.result.totalHits,
              settings.page,
              numResults,
              if (settings.language == "*") Language.AllLanguages else settings.language,
              getHits(response.result, settings.language, settings.fallback)
            ))
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        articleIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }

}
