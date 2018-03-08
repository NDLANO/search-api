/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.NdlaClient
import no.ndla.searchapi.controller.{HealthController, InternController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.service.search._
import no.ndla.searchapi.service.{ApiSearchService, ConverterService, SearchClients}
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
  extends ArticleApiClient
    with ArticleIndexService
    with ArticleSearchService
    with AudioApiClient
    with ConverterService
    with DraftApiClient
    with Elastic4sClient
    with HealthController
    with ImageApiClient
    with IndexService
    with LazyLogging
    with LearningpathApiClient
    with NdlaClient
    with SearchClients
    with SearchConverterService
    with SearchService
    with ApiSearchService
    with SearchController
    with InternController
    with SearchApiClient {
  lazy val searchController = mock[SearchController]
  lazy val healthController = mock[HealthController]
  lazy val internController = mock[InternController]
  lazy val resourcesApp = mock[ResourcesApp]

  lazy val ndlaClient = mock[NdlaClient]
  lazy val e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  lazy val draftApiClient = mock[DraftApiClient]
  lazy val learningpathApiClient = mock[LearningpathApiClient]
  lazy val imageApiClient = mock[ImageApiClient]
  lazy val audioApiClient = mock[AudioApiClient]
  lazy val articleApiClient = mock[ArticleApiClient]
  lazy val SearchClients = Map[String, SearchApiClient](
    "articles" -> draftApiClient,
    "learningpaths" -> learningpathApiClient,
    "images" -> imageApiClient,
    "audios" -> audioApiClient
  )

  val searchService = mock[ApiSearchService]
  val converterService = mock[ConverterService]
  lazy val searchConverterService = mock[SearchConverterService]
  lazy val articleSearchService = mock[ArticleSearchService]
  lazy val articleIndexService = mock[ArticleIndexService]
}
