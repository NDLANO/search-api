/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.{ArticleApiSearchResults, SearchParams}

import scala.concurrent.Future
import scala.util.Try

trait DraftApiClient {
  this: NdlaClient with SearchApiClient =>
  val draftApiClient: DraftApiClient

  class DraftApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath = "draft-api/v1/drafts"
    override val name = "articles"

    def search(searchParams: SearchParams): Future[Try[ArticleApiSearchResults]] =
      search[ArticleApiSearchResults](searchParams)
  }
}
