/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.AudioApiSearchResults

trait AudioApiClient {
  this: NdlaClient with ApiClient =>
  val audioApiClient: AudioApiClient

  class AudioApiClient(val baseUrl: String) extends SearchApiClient[AudioApiSearchResults]("audio-api/v1/audio")
}
