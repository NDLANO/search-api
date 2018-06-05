/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties.ApiGatewayUrl
import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.taxonomy._
import org.json4s.DefaultFormats

import scala.util.Try
import scalaj.http.Http

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends LazyLogging {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    private val TaxonomyApiEndpoint = s"http://$ApiGatewayUrl/taxonomy/v1"

    def getAllResources: Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/resources/").map(_.distinct)

    def getAllSubjects: Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/subjects/").map(_.distinct)

    def getAllTopics: Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/topics/").map(_.distinct)

    def getAllResourceTypes: Try[List[ResourceType]] =
      get[List[ResourceType]](s"$TaxonomyApiEndpoint/resource-types/").map(_.distinct)

    def getAllTopicResourceConnections: Try[List[TopicResourceConnection]] =
      get[List[TopicResourceConnection]](s"$TaxonomyApiEndpoint/topic-resources/").map(_.distinct)

    def getAllTopicSubtopicConnections: Try[List[TopicSubtopicConnection]] =
      get[List[TopicSubtopicConnection]](s"$TaxonomyApiEndpoint/topic-subtopics/").map(_.distinct)

    def getAllResourceResourceTypeConnections: Try[List[ResourceResourceTypeConnection]] =
      get[List[ResourceResourceTypeConnection]](s"$TaxonomyApiEndpoint/resource-resourcetypes/").map(_.distinct)

    def getAllSubjectTopicConnections: Try[List[SubjectTopicConnection]] =
      get[List[SubjectTopicConnection]](s"$TaxonomyApiEndpoint/subject-topics/").map(_.distinct)

    def getAllRelevances: Try[List[Relevance]] =
      get[List[Relevance]](s"$TaxonomyApiEndpoint/relevances/").map(_.distinct)

    def getAllFilters: Try[List[Filter]] =
      get[List[Filter]](s"$TaxonomyApiEndpoint/filters/").map(_.distinct)

    def getAllResourceFilterConnections: Try[List[ResourceFilterConnection]] =
      get[List[ResourceFilterConnection]](s"$TaxonomyApiEndpoint/resource-filters/").map(_.distinct)

    def getAllTopicFilterConnections: Try[List[TopicFilterConnection]] =
      get[List[TopicFilterConnection]](s"$TaxonomyApiEndpoint/topic-filters/").map(_.distinct)

    def getTaxonomyBundle: Try[Bundle] = {
      logger.info("Fetching taxonomy in bulk...")
      for {
        filters <- getAllFilters
        relevances <- getAllRelevances
        resourceFilterConnections <- getAllResourceFilterConnections
        resourceResourceTypeConnections <- getAllResourceResourceTypeConnections
        resourceTypes <- getAllResourceTypes
        resources <- getAllResources
        subjectTopicConnections <- getAllSubjectTopicConnections
        subjects <- getAllSubjects
        topicFilterConnections <- getAllTopicFilterConnections
        topicResourceConnections <- getAllTopicResourceConnections
        topicSubtopicConnections <- getAllTopicSubtopicConnections
        topics <- getAllTopics

        bundle <- Try(
          Bundle(
            filters,
            relevances,
            resourceFilterConnections,
            resourceResourceTypeConnections,
            resourceTypes,
            resources,
            subjectTopicConnections,
            subjects,
            topicFilterConnections,
            topicResourceConnections,
            topicSubtopicConnections,
            topics
          ))

      } yield bundle
    }

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(url).timeout(20000, 20000).params(params))
    }
  }
}
