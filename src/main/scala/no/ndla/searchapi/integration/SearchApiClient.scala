/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import com.netaporter.uri.dsl._
import no.ndla.searchapi.model.api.ApiSearchException
import no.ndla.searchapi.model.domain.{ApiSearchResults, SearchParams}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

trait SearchApiClient {
  this: NdlaClient =>

  trait SearchApiClient {
    val name: String
    val baseUrl: String
    val searchPath: String
    def search(searchParams: SearchParams): Future[Try[ApiSearchResults]]

    def get[T](path: String, params: Map[String, Any])(implicit mf: Manifest[T]): Try[T] =
      ndlaClient.fetch[T](Http((baseUrl / path).addParams(params.toList)))

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
