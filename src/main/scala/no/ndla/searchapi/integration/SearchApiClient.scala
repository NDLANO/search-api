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
import no.ndla.searchapi.model.domain.{ApiSearchResults, DomainDumpResults, SearchParams}
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
    val dumpDomainPath: String = "intern/dump/article"


    def getChunks[T](implicit mf: Manifest[T]): Stream[Try[Seq[T]]] = {
      getChunk(0, 0) match {
        case Success(initSearch) =>
          val dbCount = initSearch.totalCount
          val pageSize = SearchApiProperties.IndexBulkSize
          val numPages = ceil(dbCount.toDouble / pageSize.toDouble).toInt
          val pages = Seq.range(1, numPages + 1)

          val stream: Stream[Try[Seq[T]]] = pages.toStream.map(p => {
            getChunk[T](p, pageSize).map(_.results)
          })

          stream
        case Failure(ex) =>
          logger.error(s"Could not fetch initial chunk from $baseUrl/$dumpDomainPath")
          Stream(Failure(ex))
      }
    }

    private def getChunk[T](page: Int, pageSize: Int)(implicit mf: Manifest[T]): Try[DomainDumpResults[T]] = {
      val params = Map(
        "page" -> page,
        "page-size" -> pageSize
      )

      get[DomainDumpResults[T]](dumpDomainPath, params) match {
        case Success(result) =>
          logger.info(s"Fetched chunk of ${result.results.size}...")
          Success(result)
        case Failure(ex) =>
          logger.error(s"Could not fetch chunk on page: '$page', with pageSize: '$pageSize' from '$baseUrl/$dumpDomainPath'")
          Failure(ex)
      }
    }

    def search(searchParams: SearchParams): Future[Try[ApiSearchResults]]

    def get[T](path: String, params: Map[String, Any])(implicit mf: Manifest[T]): Try[T] = {
      implicit val formats: Formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
      ndlaClient.fetchWithForwardedAuth[T](Http((baseUrl / path).addParams(params.toList)))
    }

    protected def search[T <: ApiSearchResults](searchParams: SearchParams)(implicit mf: Manifest[T]): Future[Try[T]] = {
      val queryParams = searchParams.remaindingParams ++ Map(
        "language" -> searchParams.language,
        "sort" -> searchParams.sort,
        "page" -> searchParams.page,
        "page-size" -> searchParams.pageSize)

      Future { get(searchPath, queryParams) }.map {
        case Success(a) => Success(a)
        case Failure(ex) => Failure(new ApiSearchException(name, ex.getMessage))
      }
    }

  }
}
