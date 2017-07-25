/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import com.netaporter.uri.dsl._
import no.ndla.searchapi.model.domain.{ApiSearchResults, SearchParams}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalaj.http.Http

trait SearchApiClient {
  this: NdlaClient =>

  trait SearchApiClient {
    val baseUrl: String
    val searchPath: String

    def get[T](path: String, params: (String, Any)*)(implicit mf: Manifest[T]): Try[T] =
      ndlaClient.fetch[T](Http((baseUrl / path).addParams(params)))

    def search(searchParams: SearchParams): Future[ApiSearchResults]

    protected def search[T <: ApiSearchResults](searchParams: SearchParams)(implicit mf: Manifest[T]): Future[T] =
      Future {
        get(searchPath,
          "query" -> searchParams.query,
          "language" -> searchParams.language,
          "sort" -> searchParams.sort,
          "page" -> searchParams.page,
          "pageSize" -> searchParams.pageSize
        ).get // This will throw a exception if the result is a Failure, this error must then be handled in the onFailure or onComplete future-callback
      }

  }
}
