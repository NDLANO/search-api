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
import no.ndla.searchapi.model.domain.Language
import no.ndla.searchapi.model.taxonomy._

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
      get[TaxonomyResource](s"$TaxonomyApiEndpoint/resources/$resourceId")
    }

    /**
      * Returns sequence of names with associated language in a tuple.
      * @param subjectId Id of subject to fetch.
      * @return Sequence of tuples with (name, language)
      */
    def getSubjectNames(subjectId: String): Try[Seq[TaxonomyTranslation]] = {
      for {
        subject <- get[TaxonomySubject](
          s"$TaxonomyApiEndpoint/subjects/$subjectId")

        subjectTranslations <- get[Seq[TaxonomyTranslation]](
          s"$TaxonomyApiEndpoint/subjects/$subjectId/translations")

        result <- subjectTranslations :+ TaxonomyTranslation(
          Language.DefaultLanguage,
          subject.name)

      } yield result
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

    def getFilterConnectionsForResource(
        resourceId: String): Try[Seq[TaxonomyFilterConnection]] =
      get[Seq[TaxonomyFilterConnection]](
        s"$TaxonomyApiEndpoint/resources/$resourceId/filters")

    def getFilterConnectionsForTopic(
        topicId: String): Try[Seq[TaxonomyFilterConnection]] =
      get[Seq[TaxonomyFilterConnection]](
        s"$TaxonomyApiEndpoint/topics/$topicId/filters"
      )

    def queryResources(
        contentUri: String): Try[Seq[TaxonomyQueryResourceResult]] =
      get[Seq[TaxonomyQueryResourceResult]](
        s"$TaxonomyApiEndpoint/queries/resources/?contentURI=$contentUri")

    def queryTopics(contentUri: String): Try[Seq[TaxonomyResource]] =
      get[Seq[TaxonomyResource]](
        s"$TaxonomyApiEndpoint/queries/topics/?contentURI=$contentUri")

    def getFilter(filterId: String): Try[TaxonomyFilter] =
      get[TaxonomyFilter](s"$TaxonomyApiEndpoint/filters/$filterId")

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

        bundle <- Try(
          TaxonomyBundle(
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
