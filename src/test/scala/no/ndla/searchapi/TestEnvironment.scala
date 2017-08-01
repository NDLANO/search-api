/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.network.NdlaClient
import no.ndla.searchapi.controller.{HealthController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.service.{ConverterService, SearchService}
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
  extends HealthController
    with SearchController
    with SearchService
    with ConverterService
    with SearchApiClient
    with ArticleApiClient
    with LearningpathApiClient
    with ImageApiClient
    with AudioApiClient
    with NdlaClient
{
  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val searchController = mock[SearchController]

  val searchService = mock[SearchService]
  val converterService = mock[ConverterService]

  val ndlaClient = mock[NdlaClient]
  val articleApiClient = mock[ArticleApiClient]
  val learningpathApiClient = mock[LearningpathApiClient]
  val imageApiClient = mock[ImageApiClient]
  val audioApiClient = mock[AudioApiClient]
}
