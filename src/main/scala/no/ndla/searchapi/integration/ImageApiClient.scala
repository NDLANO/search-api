/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.ImageApiSearchResults

trait ImageApiClient {
  this: NdlaClient with ApiClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient(val baseUrl: String) extends SearchApiClient[ImageApiSearchResults]("image-api/v2/images")
}
