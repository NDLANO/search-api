/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import java.nio.file.{Files, Path}

import com.sksamuel.elastic4s.embedded.{InternalLocalNode, LocalNode}
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
    with MultiSearchService
    with AudioApiClient
    with ConverterService
    with DraftApiClient
    with Elastic4sClient
    with HealthController
    with ImageApiClient
    with TaxonomyApiClient
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
  val searchController = mock[SearchController]
  val healthController = mock[HealthController]
  val internController = mock[InternController]
  val resourcesApp = mock[ResourcesApp]

  val ndlaClient = mock[NdlaClient]
  val e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  val taxonomyApiClient = mock[TaxonomyApiClient]

  val draftApiClient = mock[DraftApiClient]
  val learningpathApiClient = mock[LearningpathApiClient]
  val imageApiClient = mock[ImageApiClient]
  val audioApiClient = mock[AudioApiClient]
  val articleApiClient = mock[ArticleApiClient]
  val SearchClients = Map[String, SearchApiClient](
    "articles" -> draftApiClient,
    "learningpaths" -> learningpathApiClient,
    "images" -> imageApiClient,
    "audios" -> audioApiClient
  )

  val searchService = mock[ApiSearchService]
  val converterService = mock[ConverterService]
  val searchConverterService = mock[SearchConverterService]
  val articleSearchService = mock[ArticleSearchService]
  val multiSearchService = mock[MultiSearchService]
  val articleIndexService = mock[ArticleIndexService]
}
