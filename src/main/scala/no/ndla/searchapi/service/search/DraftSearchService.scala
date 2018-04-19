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
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api.draft.DraftSummary
import no.ndla.searchapi.model.api.{ResultWindowTooLargeException, SearchResult}
import no.ndla.searchapi.model.domain.{Language, RequestInfo, Sort}
import no.ndla.searchapi.model.search.SearchType

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait DraftSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with DraftIndexService
    with SearchConverterService =>
  val draftSearchService: DraftSearchService

  class DraftSearchService extends LazyLogging with SearchService[DraftSummary] {
    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))

    override val searchIndex = List(SearchIndexes(SearchType.Drafts))

    override def hitToApiModel(hit: SearchHit, language: String): DraftSummary = {
      searchConverterService.hitAsDraftSummary(hit.sourceAsString, language)
    }

    def all(withIdIn: List[Long],
            language: String,
            license: Option[String],
            page: Int,
            pageSize: Int,
            sort: Sort.Value,
            articleTypes: Seq[String],
            fallback: Boolean): Try[SearchResult[DraftSummary]] =
      executeSearch(withIdIn, language, license, sort, page, pageSize, boolQuery(), articleTypes, fallback)

    def matchingQuery(query: String,
                      withIdIn: List[Long],
                      searchLanguage: String,
                      license: Option[String],
                      page: Int,
                      pageSize: Int,
                      sort: Sort.Value,
                      articleTypes: Seq[String],
                      fallback: Boolean): Try[SearchResult[DraftSummary]] = {

      val language = if (searchLanguage == Language.AllLanguages) "*" else searchLanguage
      val titleSearch = simpleStringQuery(query).field(s"title.$language", 2)
      val introSearch = simpleStringQuery(query).field(s"introduction.$language", 2)
      val contentSearch = simpleStringQuery(query).field(s"content.$language", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$language", 2)
      val notesSearch = simpleStringQuery(query).field("notes", 1)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              titleSearch,
              introSearch,
              contentSearch,
              tagSearch,
              notesSearch
            )
        )

      executeSearch(withIdIn, language, license, sort, page, pageSize, fullQuery, articleTypes, fallback)
    }

    def executeSearch(withIdIn: List[Long],
                      language: String,
                      license: Option[String],
                      sort: Sort.Value,
                      page: Int,
                      pageSize: Int,
                      queryBuilder: BoolQueryDefinition,
                      articleTypes: Seq[String],
                      fallback: Boolean): Try[SearchResult[DraftSummary]] = {

      val articleTypesFilter =
        if (articleTypes.nonEmpty) Some(constantScoreQuery(termsQuery("articleType", articleTypes))) else None

      val licenseFilter = license match {
        case None      => Some(noCopyright)
        case Some(lic) => Some(termQuery("license", lic))
      }

      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val (languageFilter, searchLanguage) = language match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          fallback match {
            case true  => (None, "*")
            case false => (Some(existsQuery(s"title.$lang")), lang)
          }
      }

      val filters = List(licenseFilter, idFilter, languageFilter, articleTypesFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > SearchApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are ${SearchApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {
        val searchExec = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(sort, searchLanguage))

        e4sClient.execute(searchExec) match {
          case Success(response) =>
            Success(
              SearchResult[DraftSummary](
                response.result.totalHits,
                page,
                numResults,
                if (searchLanguage == "*") Language.AllLanguages else searchLanguage,
                getHits(response.result, language, fallback)
              ))
          case Failure(ex) =>
            errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      val threadPoolSize = if (searchIndex.nonEmpty) searchIndex.size else 1
      implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))
      val requestInfo = RequestInfo()

      val draftFuture = Future {
        requestInfo.setRequestInfo()
        draftIndexService.indexDocuments
      }

      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.Drafts), draftFuture)
    }
  }
}
