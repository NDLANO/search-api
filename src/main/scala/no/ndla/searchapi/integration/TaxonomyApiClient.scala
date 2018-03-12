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

import scala.util.{Failure, Success, Try}
import scalaj.http.Http

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats
    private val TaxonomyApiEndpoint = s"http://$ApiGatewayUrl/taxonomy/v1"

    def getResource(nodeId: String): Try[TaxonomyResource] = {
      val resourceId = s"urn:resource:1:$nodeId"
      get[TaxonomyResource](s"$TaxonomyApiEndpoint/resources/$resourceId") match {
        case Failure(ex) =>
          Failure(ex)
        case Success(a) =>
          Success(a)
      }
    }

    def getAllResources: Try[Seq[TaxonomyResource]] =
      get[Seq[TaxonomyResource]](s"$TaxonomyApiEndpoint/resources/")

    def getAllSubjects: Try[Seq[TaxonomyResource]] =
      get[Seq[TaxonomyResource]](s"$TaxonomyApiEndpoint/subjects/")

    def getAllTopics: Try[Seq[TaxonomyResource]] =
      get[Seq[TaxonomyResource]](s"$TaxonomyApiEndpoint/topics/")

    def getAllResourceTypes: Try[Seq[TaxonomyResourceType]] =
      get[Seq[TaxonomyResourceType]](s"$TaxonomyApiEndpoint/resource-types/")

    def getAllTopicResourceConnections
      : Try[Seq[TaxonomyTopicResourceConnection]] =
      get[Seq[TaxonomyTopicResourceConnection]](
        s"$TaxonomyApiEndpoint/topic-resources/")

    def getAllTopicSubtopicConnections
      : Try[Seq[TaxonomyTopicSubtopicConnection]] =
      get[Seq[TaxonomyTopicSubtopicConnection]](
        s"$TaxonomyApiEndpoint/topic-subtopics/")

    def getAllResourceResourceTypeConnections
      : Try[Seq[TaxonomyResourceResourceTypeConnection]] =
      get[Seq[TaxonomyResourceResourceTypeConnection]](
        s"$TaxonomyApiEndpoint/resource-resourcetypes/")

    def getAllSubjectTopicConnections
      : Try[Seq[TaxonomySubjectTopicConnection]] =
      get[Seq[TaxonomySubjectTopicConnection]](
        s"$TaxonomyApiEndpoint/subject-topics/")

    def getAllRelevances: Try[Seq[TaxonomyRelevance]] =
      get[Seq[TaxonomyRelevance]](s"$TaxonomyApiEndpoint/relevances/")

    def getAllFilters: Try[Seq[TaxonomyFilter]] =
      get[Seq[TaxonomyFilter]](s"$TaxonomyApiEndpoint/filters/")

    def getAllResourceFilterConnections
      : Try[Seq[TaxonomyResourceFilterConnection]] =
      get[Seq[TaxonomyResourceFilterConnection]](
        s"$TaxonomyApiEndpoint/resource-filters/")

    def getAllTopicFilterConnections: Try[Seq[TaxonomyTopicFilterConnection]] =
      get[Seq[TaxonomyTopicFilterConnection]](
        s"$TaxonomyApiEndpoint/topic-filters/")

    def getTaxonomyBundle: Try[TaxonomyBundle] = {
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

        bundle <- Try(TaxonomyBundle(
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

    private def get[A](url: String, params: (String, String)*)(
        implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(url).params(params))
    }
  }
}

case class TaxonomyBundle(
    filters: Seq[TaxonomyFilter],
    relevances: Seq[TaxonomyRelevance],
    resourceFilterConnections: Seq[TaxonomyResourceFilterConnection],
    resourceResourceTypeConnections: Seq[TaxonomyResourceResourceTypeConnection],
    resourceTypes: Seq[TaxonomyResourceType],
    resources: Seq[TaxonomyResource],
    subjectTopicConnections: Seq[TaxonomySubjectTopicConnection],
    subjects: Seq[TaxonomyResource],
    topicFilterConnections: Seq[TaxonomyTopicFilterConnection],
    topicResourceConnections: Seq[TaxonomyTopicResourceConnection],
    topicSubtopicConnections: Seq[TaxonomyTopicSubtopicConnection],
    topics: Seq[TaxonomyResource]
)

case class TaxonomyResourceFilterConnection(resourceId: String,
                                            filterId: String,
                                            id: String,
                                            relevanceId: String)

case class TaxonomyTopicFilterConnection(topicId: String,
                                         filterId: String,
                                         id: String,
                                         relevanceId: String)

case class TaxonomyRelevance(id: String, name: String)

case class TaxonomyFilter(id: String, name: String, subjectId: String)

case class TaxonomyTopicResourceConnection(topicid: String,
                                           resourceId: String,
                                           id: String,
                                           primary: Boolean,
                                           rank: Int)

case class TaxonomyTopicSubtopicConnection(topicid: String,
                                           subtopicid: String,
                                           id: String,
                                           primary: Boolean,
                                           rank: Int)

case class TaxonomyResourceResourceTypeConnection(resourceId: String,
                                                  resourceTypeId: String,
                                                  id: String)

case class TaxonomySubjectTopicConnection(subjectid: String,
                                          topicid: String,
                                          id: String,
                                          primary: Boolean,
                                          rank: Int)

case class TaxonomyResourceType(id: String,
                                name: String,
                                subtypes: Option[Seq[TaxonomyResourceType]])

case class TaxonomyResource(id: String,
                            name: String,
                            contentUri: Option[String],
                            path: String)
