/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.{ArticleApiSearchResults, SearchParams}

import scala.concurrent.Future
import scala.util.Try

trait ArticleApiClient {
  this: NdlaClient with SearchApiClient =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath = "article-api/v2/articles"
    override val name = "articles"
    override val dumpDomainPath = "intern/dump/article"

    def search(searchParams: SearchParams): Future[Try[ArticleApiSearchResults]] =
      search[ArticleApiSearchResults](searchParams)
  }
}
