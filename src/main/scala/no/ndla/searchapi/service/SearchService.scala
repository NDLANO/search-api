/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.domain.{ApiSearchResults, SearchParams}
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.api.SearchResults
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait SearchService {
  this: ConverterService with SearchApiClient with ArticleApiClient with LearningpathApiClient with ImageApiClient with AudioApiClient =>
  val searchService: SearchService

  class SearchService {
    def search(searchParams: SearchParams): Seq[SearchResults] = {
      val articleFuture = apiSearch(articleApiClient, searchParams)
      val lpFuture = apiSearch(learningpathApiClient, searchParams)
      val imageFuture = apiSearch(imageApiClient, searchParams)
      val audioFuture = apiSearch(audioApiClient, searchParams)

      // TODO: error handling
      Seq(articleFuture, lpFuture, imageFuture, audioFuture)
        .map(searchResult => Await.result(searchResult, 5 seconds))
    }

    private def apiSearch(client: SearchApiClient, searchParams: SearchParams): Future[api.SearchResults] =
      client.search(searchParams).map(converterService.searchResultToApiModel)
  }
}
