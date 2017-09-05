/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.api.{ApiSearchException, SearchResults}
import no.ndla.searchapi.model.domain.SearchParams
import scala.language.postfixOps
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait SearchService {
  this: ConverterService with SearchApiClient with ArticleApiClient with LearningpathApiClient with ImageApiClient with AudioApiClient =>
  val searchService: SearchService

  class SearchService {
    def search(searchParams: SearchParams, apisToSearchIn: Set[SearchApiClient]): Seq[SearchResults] = {
      val searchResults = apisToSearchIn.map(_.search(searchParams)).toSeq
      searchResults.map(searchResult => Await.result(searchResult, 5 seconds))
        .map {
          case Success(s) => converterService.searchResultToApiModel(s)
          case Failure(ex: ApiSearchException) => api.SearchError(ex.apiName, ex.getMessage)
          case Failure(ex) => api.SearchError("unknown", ex.getMessage)
        }
    }

  }
}
