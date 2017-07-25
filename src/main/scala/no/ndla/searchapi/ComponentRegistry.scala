/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.network.NdlaClient
import no.ndla.searchapi.controller.{HealthController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.SearchApiProperties.{ArticleApiUrl, AudioApiUrl, ImageApiUrl, LearningpathApiUrl}
import no.ndla.searchapi.service.{ConverterService, SearchService}

object ComponentRegistry
  extends SearchController
    with HealthController
    with NdlaClient
    with SearchApiClient
    with ArticleApiClient
    with LearningpathApiClient
    with ImageApiClient
    with AudioApiClient
    with ConverterService
    with SearchService
{
  implicit val swagger = new SearchSwagger

  lazy val searchController = new SearchController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp

  lazy val ndlaClient = new NdlaClient
  lazy val articleApiClient = new ArticleApiClient(ArticleApiUrl)
  lazy val learningpathApiClient = new LearningpathApiClient(LearningpathApiUrl)
  lazy val imageApiClient = new ImageApiClient(ImageApiUrl)
  lazy val audioApiClient = new AudioApiClient(AudioApiUrl)

  lazy val converterService = new ConverterService
  lazy val searchService = new SearchService
}
