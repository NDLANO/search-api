/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.{LearningpathApiSearchResults, SearchParams}

import scala.concurrent.Future
import scala.util.Try

trait LearningpathApiClient {
  this: NdlaClient with SearchApiClient =>
  val learningpathApiClient: LearningpathApiClient

  class LearningpathApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath = "learningpath-api/v2/learningpaths"
    override val name = "learningpaths"

    def search(searchParams: SearchParams): Future[Try[LearningpathApiSearchResults]] =
      search[LearningpathApiSearchResults](searchParams)
  }
}
