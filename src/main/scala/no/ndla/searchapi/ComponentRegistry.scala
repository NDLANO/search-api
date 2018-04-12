/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.NdlaClient
import no.ndla.searchapi.controller.{HealthController, InternController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.SearchApiProperties._
import no.ndla.searchapi.service.search._
import no.ndla.searchapi.service.{ApiSearchService, ConverterService, SearchClients, search}

object ComponentRegistry
    extends ArticleApiClient
    with ArticleIndexService
    with ArticleSearchService
    with LearningPathIndexService
    with LearningPathSearchService
    with DraftIndexService
    with DraftSearchService
    with MultiSearchService
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
    with SearchApiClient
{
  implicit val swagger = new SearchSwagger

  lazy val searchController = new SearchController
  lazy val healthController = new HealthController
  lazy val internController = new InternController
  lazy val resourcesApp = new ResourcesApp

  lazy val ndlaClient = new NdlaClient
  lazy val e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient()

  lazy val taxonomyApiClient = new TaxonomyApiClient

  lazy val draftApiClient = new DraftApiClient(DraftApiUrl)
  lazy val learningPathApiClient = new LearningPathApiClient(LearningpathApiUrl)
  lazy val imageApiClient = new ImageApiClient(ImageApiUrl)
  lazy val audioApiClient = new AudioApiClient(AudioApiUrl)
  lazy val articleApiClient = new ArticleApiClient(ArticleApiUrl)
  lazy val SearchClients = Map[String, SearchApiClient](
    draftApiClient.name -> draftApiClient,
    learningPathApiClient.name -> learningPathApiClient,
    imageApiClient.name -> imageApiClient,
    audioApiClient.name -> audioApiClient
  )

  lazy val searchService = new ApiSearchService
  lazy val converterService = new ConverterService
  lazy val searchConverterService = new SearchConverterService
  lazy val articleSearchService = new ArticleSearchService
  lazy val multiSearchService = new MultiSearchService
  lazy val articleIndexService = new ArticleIndexService
  lazy val learningPathIndexService = new LearningPathIndexService
  lazy val learningPathSearchService = new LearningPathSearchService
  lazy val draftIndexService = new DraftIndexService
  lazy val draftSearchService = new DraftSearchService
}
