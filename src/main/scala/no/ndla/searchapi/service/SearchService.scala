/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.api.SearchResults
import no.ndla.searchapi.model.domain.SearchParams

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

trait SearchService {
  this: ConverterService with SearchApiClient with ArticleApiClient with LearningpathApiClient with ImageApiClient with AudioApiClient =>
  val searchService: SearchService

  class SearchService {
    def search(searchParams: SearchParams): Seq[SearchResults] = {
      val articles = apiSearch(articleApiClient, searchParams)
      val learningpaths = apiSearch(learningpathApiClient, searchParams)
      val images = apiSearch(imageApiClient, searchParams)
      val audios = apiSearch(audioApiClient, searchParams)

      Seq(articles, learningpaths, images, audios)
        .map(searchResult => Await.result(searchResult, 5 seconds))
    }

    private def apiSearch(client: SearchApiClient, searchParams: SearchParams): Future[api.SearchResults] =
      client.search(searchParams).map {
        case Success(s) => converterService.searchResultToApiModel(s, client.name)
        case Failure(ex) => api.SearchError(client.name, ex.getMessage)
      }
  }
}
