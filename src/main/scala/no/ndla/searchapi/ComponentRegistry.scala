/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.network.NdlaClient
import no.ndla.searchapi.SearchApiProperties.DatabaseDetails.{ArticleApi, DraftApi, LearningpathApi}
import no.ndla.searchapi.controller.{HealthController, InternController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.SearchApiProperties._
import no.ndla.searchapi.auth.User
import no.ndla.searchapi.repository.{ArticleRepository, DraftRepository, LearningpathRepository}
import no.ndla.searchapi.service.search._
import no.ndla.searchapi.service.{ApiSearchService, ConverterService, SearchClients, search}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
    extends ArticleApiClient
    with ArticleIndexService
    with LearningPathIndexService
    with DraftIndexService
    with MultiSearchService
    with MultiDraftSearchService
    with AudioApiClient
    with ConverterService
    with DraftApiClient
    with Elastic4sClient
    with HealthController
    with TaxonomyApiClient
    with ImageApiClient
    with IndexService
    with LazyLogging
    with LearningPathApiClient
    with NdlaClient
    with SearchClients
    with SearchConverterService
    with SearchService
    with ApiSearchService
    with SearchController
    with InternController
    with User
    with SearchApiClient
    with GrepApiClient
    with DataSources
    with ArticleRepository
    with DraftRepository
    with LearningpathRepository {
  implicit val swagger = new SearchSwagger

  lazy val searchController = new SearchController
  lazy val healthController = new HealthController
  lazy val internController = new InternController
  lazy val resourcesApp = new ResourcesApp

  lazy val ndlaClient = new NdlaClient
  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient()

  lazy val taxonomyApiClient = new TaxonomyApiClient
  lazy val grepApiClient = new GrepApiClient

  lazy val draftApiClient = new DraftApiClient(DraftApiUrl)
  lazy val learningPathApiClient = new LearningPathApiClient(LearningpathApiUrl)
  lazy val imageApiClient = new ImageApiClient(ImageApiUrl)
  lazy val audioApiClient = new AudioApiClient(AudioApiUrl)
  lazy val articleApiClient = new ArticleApiClient(ArticleApiUrl)
  lazy val SearchClients: Map[String, SearchApiClient] = Map(
    draftApiClient.name -> draftApiClient,
    learningPathApiClient.name -> learningPathApiClient,
    imageApiClient.name -> imageApiClient,
    audioApiClient.name -> audioApiClient
  )

  lazy val searchService = new ApiSearchService
  lazy val converterService = new ConverterService
  lazy val searchConverterService = new SearchConverterService
  lazy val multiSearchService = new MultiSearchService
  lazy val articleIndexService = new ArticleIndexService
  lazy val learningPathIndexService = new LearningPathIndexService
  lazy val draftIndexService = new DraftIndexService
  lazy val multiDraftSearchService = new MultiDraftSearchService

  lazy val user = new User

  lazy val articleRepository = new ArticleRepository
  lazy val draftRepository = new DraftRepository
  lazy val learningpathRepository = new LearningpathRepository

  override val articleApiDataSource: HikariDataSource = DataSource.ArticleApiDataSource
  override val draftApiDataSource: HikariDataSource = DataSource.DraftApiDataSource
  override val learningpathApiDataSource: HikariDataSource = DataSource.LearningpathApiDataSource

  def connectToDatabases(): Unit = {
    ConnectionPool.add(ArticleApi.connectionPoolName, new DataSourceConnectionPool(articleApiDataSource))
    ConnectionPool.add(DraftApi.connectionPoolName, new DataSourceConnectionPool(draftApiDataSource))
    ConnectionPool.add(LearningpathApi.connectionPoolName, new DataSourceConnectionPool(learningpathApiDataSource))
  }

  connectToDatabases()
}
