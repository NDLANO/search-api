/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.model.api.ApiSearchException
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.searchapi.model.domain.{ApiSearchResults, DomainDumpResults, SearchParams}
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Formats}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

import scala.math.ceil

trait SearchApiClient {
  this: NdlaClient with LazyLogging =>

  trait SearchApiClient {
    val name: String
    val baseUrl: String
    val searchPath: String
    val dumpDomainPath: String = s"intern/dump/$name"

    def getChunks[T](implicit mf: Manifest[T]): Iterator[Try[Seq[T]]] = {
      getChunk(0, 0) match {
        case Success(initSearch) =>
          val dbCount = initSearch.totalCount
          val pageSize = SearchApiProperties.IndexBulkSize
          val numPages = ceil(dbCount.toDouble / pageSize.toDouble).toInt
          val pages = Seq.range(1, numPages + 1)

          val iterator: Iterator[Try[Seq[T]]] = pages.toIterator.map(p => {
            getChunk[T](p, pageSize).map(_.results)
          })

          iterator
        case Failure(ex) =>
          logger.error(s"Could not fetch initial chunk from $baseUrl/$dumpDomainPath")
          Iterator(Failure(ex))
      }
    }

    private def getChunk[T](page: Int, pageSize: Int)(implicit mf: Manifest[T]): Try[DomainDumpResults[T]] = {
      val params = Map(
        "page" -> page,
        "page-size" -> pageSize
      )

      get[DomainDumpResults[T]](dumpDomainPath, params, timeout = 20000) match {
        case Success(result) =>
          logger.info(s"Fetched chunk of ${result.results.size} $name...")
          Success(result)
        case Failure(ex) =>
          logger.error(
            s"Could not fetch chunk on page: '$page', with pageSize: '$pageSize' from '$baseUrl/$dumpDomainPath'")
          Failure(ex)
      }
    }

    def search(searchParams: SearchParams): Future[Try[ApiSearchResults]]

    def get[T](path: String, params: Map[String, Any], timeout: Int = 5000)(implicit mf: Manifest[T]): Try[T] = {
      implicit val formats: Formats =
        org.json4s.DefaultFormats +
          new EnumNameSerializer(LearningPathStatus) +
          new EnumNameSerializer(LearningPathVerificationStatus) +
          new EnumNameSerializer(StepType) +
          new EnumNameSerializer(StepStatus) +
          new EnumNameSerializer(EmbedType) +
          new EnumNameSerializer(ArticleStatus) +
          new EnumNameSerializer(LearningResourceType) ++
          org.json4s.ext.JodaTimeSerializers.all

      ndlaClient.fetchWithForwardedAuth[T](Http((baseUrl / path).addParams(params.toList)).timeout(timeout, timeout))
    }

    protected def search[T <: ApiSearchResults](searchParams: SearchParams)(
        implicit mf: Manifest[T]): Future[Try[T]] = {
      val queryParams = searchParams.remaindingParams ++ Map("language" -> searchParams.language,
                                                             "sort" -> searchParams.sort,
                                                             "page" -> searchParams.page,
                                                             "page-size" -> searchParams.pageSize)

      Future { get(searchPath, queryParams) }.map {
        case Success(a)  => Success(a)
        case Failure(ex) => Failure(new ApiSearchException(name, ex.getMessage))
      }
    }

  }
}
