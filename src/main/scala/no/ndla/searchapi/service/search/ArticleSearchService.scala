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
import no.ndla.searchapi.SearchApiProperties.SearchIndexes
import no.ndla.searchapi.model.api
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api.{ResultWindowTooLargeException, SearchResult}
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.domain.{Language, RequestInfo, Sort}
import no.ndla.searchapi.model.search.SearchType

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait ArticleSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with ArticleIndexService =>
  val articleSearchService: ArticleSearchService

  class ArticleSearchService extends LazyLogging with SearchService[ArticleSummary] {
    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))

    override val searchIndex = List(SearchIndexes(SearchType.Articles))

    override def hitToApiModel(hit: SearchHit, language: String): ArticleSummary = {
      searchConverterService.hitAsArticleSummary(hit.sourceAsString, language)
    }

    def all(withIdIn: List[Long],
            language: String,
            license: Option[String],
            page: Int,
            pageSize: Int,
            sort: Sort.Value,
            articleTypes: Seq[String],
            fallback: Boolean): Try[SearchResult[ArticleSummary]] = {
      executeSearch(withIdIn, language, license, sort, page, pageSize, boolQuery(), articleTypes, fallback)
    }

    def matchingQuery(query: String,
                      withIdIn: List[Long],
                      searchLanguage: String,
                      license: Option[String],
                      page: Int,
                      pageSize: Int,
                      sort: Sort.Value,
                      articleTypes: Seq[String],
                      fallback: Boolean): Try[SearchResult[ArticleSummary]] = {
      val language = if (searchLanguage == Language.AllLanguages || fallback) "*" else searchLanguage
      val titleSearch = simpleStringQuery(query).field(s"title.$language", 2)
      val introSearch = simpleStringQuery(query).field(s"introduction.$language", 2)
      val metaSearch = simpleStringQuery(query).field(s"metaDescription.$language", 1)
      val contentSearch = simpleStringQuery(query).field(s"content.$language", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$language", 1)

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

      executeSearch(withIdIn, searchLanguage, license, sort, page, pageSize, fullQuery, articleTypes, fallback)
    }

    def executeSearch(withIdIn: List[Long],
                      language: String,
                      license: Option[String],
                      sort: Sort.Value,
                      page: Int,
                      pageSize: Int,
                      queryBuilder: BoolQueryDefinition,
                      articleTypes: Seq[String],
                      fallback: Boolean): Try[api.SearchResult[ArticleSummary]] = {

      val articleTypesFilter = if (articleTypes.nonEmpty) Some(constantScoreQuery(termsQuery("articleType", articleTypes))) else None
      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val licenseFilter = license match {
        case None => Some(noCopyright)
        case Some(lic) => Some(termQuery("license", lic))
      }

      val (languageFilter, searchLanguage) = language match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          fallback match {
            case true => (None, "*")
            case false => (Some(existsQuery(s"title.$lang")), lang)
          }
      }

      val filters = List(licenseFilter, idFilter, languageFilter, articleTypesFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > SearchApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${SearchApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {

        val searchToExec = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(sort, searchLanguage))

        e4sClient.execute(searchToExec) match {
          case Success(response) =>
            Success(api.SearchResult[ArticleSummary](
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

      val articleFuture = Future {
        requestInfo.setRequestInfo()
        articleIndexService.indexDocuments
      }

      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.Articles), articleFuture)
    }

  }

}
