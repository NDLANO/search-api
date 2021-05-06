/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.network.NdlaClient
import no.ndla.searchapi.auth.User
import no.ndla.searchapi.controller.{HealthController, InternController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.repository.{ArticleRepository, DraftRepository, LearningpathRepository}
import no.ndla.searchapi.service.search._
import no.ndla.searchapi.service.{ApiSearchService, ConverterService, SearchClients}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends ArticleApiClient
    with MockitoSugar
    with ArticleIndexService
    with MultiSearchService
    with DraftIndexService
    with MultiDraftSearchService
    with AudioApiClient
    with ConverterService
    with DraftApiClient
    with Elastic4sClient
    with HealthController
    with ImageApiClient
    with TaxonomyApiClient
    with IndexService
    with LazyLogging
    with LearningPathApiClient
    with NdlaClient
    with SearchClients
    with SearchConverterService
    with SearchService
    with ApiSearchService
    with SearchController
    with User
    with LearningPathIndexService
    with DataSources
    with ArticleRepository
    with DraftRepository
    with LearningpathRepository
    with InternController
    with SearchApiClient
    with GrepApiClient {
  val searchController = mock[SearchController]
  val healthController = mock[HealthController]
  val internController = mock[InternController]
  val resourcesApp = mock[ResourcesApp]

  val ndlaClient = mock[NdlaClient]
  var e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  val taxonomyApiClient = mock[TaxonomyApiClient]
  val grepApiClient = mock[GrepApiClient]

  val draftApiClient = mock[DraftApiClient]
  val learningPathApiClient = mock[LearningPathApiClient]
  val imageApiClient = mock[ImageApiClient]
  val audioApiClient = mock[AudioApiClient]
  val articleApiClient = mock[ArticleApiClient]

  val SearchClients = Map[String, SearchApiClient](
    "articles" -> draftApiClient,
    "learningpaths" -> learningPathApiClient,
    "images" -> imageApiClient,
    "audios" -> audioApiClient
  )

  val searchService = mock[ApiSearchService]
  val converterService = mock[ConverterService]
  val searchConverterService = mock[SearchConverterService]
  val multiSearchService = mock[MultiSearchService]
  val articleIndexService = mock[ArticleIndexService]
  val learningPathIndexService = mock[LearningPathIndexService]
  val draftIndexService = mock[DraftIndexService]
  val multiDraftSearchService = mock[MultiDraftSearchService]

  val user = mock[User]

  val articleRepository = mock[ArticleRepository]
  val draftRepository = mock[DraftRepository]
  val learningpathRepository = mock[LearningpathRepository]

  val articleApiDataSource: HikariDataSource = DataSource.ArticleApiDataSource // TODO: Mock dissa sikkert
  val draftApiDataSource: HikariDataSource = DataSource.DraftApiDataSource
  val learningpathApiDataSource: HikariDataSource = DataSource.LearningpathApiDataSource
}
