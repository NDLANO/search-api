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
import no.ndla.searchapi.service.search.{ArticleIndexService, IndexService, SearchConverterService}
import no.ndla.searchapi.service.{ConverterService, SearchClients, SearchService}

object ComponentRegistry
    extends ArticleApiClient
    with ArticleIndexService
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

  lazy val draftApiClient = new DraftApiClient(DraftApiUrl)
  lazy val learningpathApiClient = new LearningpathApiClient(LearningpathApiUrl)
  lazy val imageApiClient = new ImageApiClient(ImageApiUrl)
  lazy val audioApiClient = new AudioApiClient(AudioApiUrl)
  lazy val articleApiClient = new ArticleApiClient(ArticleApiUrl)
  lazy val SearchClients = Map[String, SearchApiClient](
    draftApiClient.name -> draftApiClient,
    learningpathApiClient.name -> learningpathApiClient,
    imageApiClient.name -> imageApiClient,
    audioApiClient.name -> audioApiClient
  )

  lazy val converterService = new ConverterService
  lazy val searchConverterService = new SearchConverterService
  lazy val searchService = new SearchService
  lazy val articleIndexService = new ArticleIndexService
}
