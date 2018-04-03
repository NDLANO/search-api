/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.util.concurrent.Executors

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.sksamuel.elastic4s.searches.sort.{FieldSortDefinition, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api.learningpath.LearningPathSummary
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.api.ResultWindowTooLargeException
import no.ndla.searchapi.model.domain.{Language, Sort}
import no.ndla.searchapi.model.search.SearchableLearningPath
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.native.Serialization._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait LearningPathSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with LearningPathIndexService =>
  val learningPathSearchService: LearningPathSearchService

  class LearningPathSearchService extends LazyLogging with SearchService[LearningPathSummary] {
    override val searchIndex: String = SearchApiProperties.SearchIndexes("learningpaths")

    override def hitToApiModel(hit: SearchHit, language: String): LearningPathSummary = {
      searchConverterService.asLearningPathSummary()

    }

    def all(withIdIn: List[Long],
              taggedWith: Option[String],
              sort: Sort.Value,
              language: Option[String],
              page: Option[Int],
              pageSize: Option[Int],
              fallback: Boolean): Try[api.SearchResult[LearningPathSummary]] = {
      val searchLanguage = language match {
        case None | Some(Language.AllLanguages) => "*"
        case Some(lang) => lang
      }

      val fullQuery = searchLanguage match {
        case "*" => boolQuery()
        case lang => {
          val titleSearch = existsQuery(s"title.$lang")
          val descSearch = existsQuery(s"description.$lang")

          boolQuery()
            .should(
              nestedQuery("title", titleSearch).scoreMode(ScoreMode.Avg),
              nestedQuery("description", descSearch).scoreMode(ScoreMode.Avg)
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

    def matchingQuery(withIdIn: List[Long],
                      query: String,
                      taggedWith: Option[String],
                      language: Option[String],
                      sort: Sort.Value,
                      page: Option[Int],
                      pageSize: Option[Int],
                      fallback: Boolean
                     ): Try[api.SearchResult[LearningPathSummary]] = {
      val searchLanguage = language match {
        case None | Some(Language.AllLanguages) => "*"
        case Some(lang) => lang
      }

      val titleSearch = simpleStringQuery(query).field(s"title.$searchLanguage", 2)
      val descSearch = simpleStringQuery(query).field(s"description.$searchLanguage", 2)
      val stepTitleSearch = simpleStringQuery(query).field(s"learningsteps.title.$searchLanguage", 1)
      val stepDescSearch = simpleStringQuery(query).field(s"learningsteps.description.$searchLanguage", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$searchLanguage", 2)
      val authorSearch = simpleStringQuery(query).field("author", 1)

      val hi = highlight("*").preTag("").postTag("").numberOfFragments(0)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              nestedQuery("title", titleSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("title").highlighting(hi)),
              nestedQuery("description", descSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("description").highlighting(hi)),
              nestedQuery("learningsteps.title", stepTitleSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("learningsteps.title").highlighting(hi)),
              nestedQuery("learningsteps.description", stepDescSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("learningsteps.description").highlighting(hi)),
              nestedQuery("tags", tagSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("tags").highlighting(hi)),
              authorSearch
            )
        )

      executeSearch(
        fullQuery,
        withIdIn,
        taggedWith,
        sort,
        searchLanguage,
        page,
        pageSize,
        fallback)
    }

    def executeSearch(queryBuilder: BoolQueryDefinition,
                      withIdIn: List[Long],
                      taggedWith: Option[String],
                      sort: Sort.Value,
                      language: String,
                      page: Option[Int],
                      pageSize: Option[Int],
                      fallback: Boolean
                     ): Try[api.SearchResult[LearningPathSummary]] = {

      val tagFilter = taggedWith match {
        case None => None
        case Some(tag) => Some(nestedQuery("tags", termQuery(s"tags.$language.raw", tag)).scoreMode(ScoreMode.None))
      }
      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val filters = List(tagFilter, idFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page.getOrElse(1), pageSize.getOrElse(100))
      val requestedResultWindow = page.getOrElse(1) * numResults
      if (requestedResultWindow > SearchApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${SearchApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {
        val searchToExecute = search(searchIndex)
            .size(numResults)
            .from(startAt)
            .query(filteredSearch)
            .sortBy(getSortDefinition(sort, language))

        e4sClient.execute(searchToExecute) match {
          case Success(response) =>
            Success(api.SearchResult[LearningPathSummary](
              response.result.totalHits,
              page.getOrElse(1),
              numResults,
              if(language == "*") Language.AllLanguages else language,
              getHits(response.result, language, fallback)
            ))
          case Failure(ex) =>
            Failure(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        learningPathIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }

}
