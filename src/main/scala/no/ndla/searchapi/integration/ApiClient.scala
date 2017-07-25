/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import com.netaporter.uri.dsl._
import no.ndla.searchapi.model.domain.SearchParams

import scala.util.Try
import scalaj.http.Http

trait ApiClient {
  this: NdlaClient =>

  abstract class SearchApiClient[T](searchPath: String) {
    val baseUrl: String

    def get(path: String, params: (String, Any)*): Try[T] =
      ndlaClient.fetch[T](Http((baseUrl / path).addParams(params)))

    def search(searchParams: SearchParams): Try[T] =
      get(searchPath,
        "query" -> searchParams.query,
        "language" -> searchParams.language,
        "sort" -> searchParams.sort,
        "page" -> searchParams.page,
        "pageSize" -> searchParams.pageSize
      )

  }
}
