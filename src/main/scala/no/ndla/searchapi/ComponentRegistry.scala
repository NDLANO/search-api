/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.searchapi.controller.{HealthController, SearchController}
import no.ndla.searchapi.integration.{ArticleApiClient, AudioApiClient, ImageApiClient, LearningpathApiClient}
import no.ndla.searchapi.SearchApiProperties.{ArticleApiUrl, LearningpathApiUrl, ImageApiUrl, AudioApiUrl}

object ComponentRegistry
  extends SearchController
  with HealthController
  with ArticleApiClient
  with LearningpathApiClient
  with ImageApiClient
  with AudioApiClient
{
  implicit val swagger = new SearchSwagger

  lazy val searchController = new SearchController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp

  lazy val articleApiClient = new ArticleApiClient(ArticleApiUrl)
  lazy val LearningpathApiClient = new LearningpathApiClient(LearningpathApiUrl)
  lazy val imageApiClient = new ImageApiClient(ImageApiUrl)
  lazy val audioApiClient = new AudioApiClient(AudioApiUrl)
}
