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
import no.ndla.searchapi.service.{ConverterService, SearchClients, ApiSearchService}
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
  extends HealthController
    with SearchController
    with ApiSearchService
    with SearchClients
    with ConverterService
    with SearchApiClient
    with DraftApiClient
    with LearningpathApiClient
    with ImageApiClient
    with AudioApiClient
    with NdlaClient
{
  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val searchController = mock[SearchController]

  val searchService = mock[ApiSearchService]
  val converterService = mock[ConverterService]

  val ndlaClient = mock[NdlaClient]
  val draftApiClient = mock[DraftApiClient]
  val learningpathApiClient = mock[LearningpathApiClient]
  val imageApiClient = mock[ImageApiClient]
  val audioApiClient = mock[AudioApiClient]
  lazy val SearchClients = Map[String, SearchApiClient](
    "articles" -> draftApiClient,
    "learningpaths" -> learningpathApiClient,
    "images" -> imageApiClient,
    "audios" -> audioApiClient
  )
}
