/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.{ImageApiSearchResults, SearchParams}

import scala.concurrent.Future

trait ImageApiClient {
  this: NdlaClient with SearchApiClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath = "image-api/v2/images"

    def search(searchParams: SearchParams): Future[ImageApiSearchResults] =
      search[ImageApiSearchResults](searchParams)
  }
}
