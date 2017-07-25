/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.LearningpathApiSearchResults

trait LearningpathApiClient {
  this: NdlaClient with ApiClient =>
  val LearningpathApiClient: LearningpathApiClient

  class LearningpathApiClient(val baseUrl: String) extends SearchApiClient[LearningpathApiSearchResults]("learningpath-api/v2/learningpaths")
}
