/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.ArticleApiSearchResults

trait ArticleApiClient {
  this: NdlaClient with ApiClient =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient(val baseUrl: String) extends SearchApiClient[ArticleApiSearchResults]("article-api/v2/articles")
}
