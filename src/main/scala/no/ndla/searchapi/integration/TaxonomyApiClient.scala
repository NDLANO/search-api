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

    def getResource(resourceId: String): Try[Resource] = {
      get[Resource](s"$TaxonomyApiEndpoint/resources/$resourceId")
    }

    def getTopic(topicId: String): Try[Resource] = {
      get[Resource](s"$TaxonomyApiEndpoint/topics/$topicId")
    }

    def getSubject(subjectId: String): Try[Resource] = {
      get[Resource](s"$TaxonomyApiEndpoint/subjects/$subjectId")
    }


    def getRelevanceById(relevanceId: String): Try[Relevance] = {
      get[Relevance](s"$TaxonomyApiEndpoint/relevances/$relevanceId")
    }

    def getBreadcrumbs(path: String, language: String): Try[Seq[String]] = {
      resolvePath(path) match {
        case Success(resolved) =>
          val fetchedTranslations = resolved.parents.map(pid => getTranslations(pid))
          val pathTranslations = fetchedTranslations.collect{case Success(x) => x}
          val failedTranslations = fetchedTranslations.collect{case Failure(ex) => Failure(ex)}

          if(failedTranslations.nonEmpty) {
            failedTranslations.head
          } else {
            val bestTranslations = pathTranslations.map(objectTranslations => {
              Language.findByLanguageOrBestEffort(objectTranslations, language)
            })

            if(bestTranslations.contains(None)){
              Failure(new RuntimeException(s"We could not find translation for $path")) // TODO: better exception
            } else {
              Success(bestTranslations.flatten.map(_.name))
            }
          }
        case Failure(ex) => Failure(ex)
      }
    }

    /**
      * Returns sequence of names with associated language in a tuple.
      * @param resourceId Id of resource to fetch.
      * @return Sequence of tuples with (name, language)
      */
    def getResourceTranslations(resourceId: String): Try[Seq[Translation]] = {
      for {
        topic <- getTopic(resourceId)
        translations <- get[Seq[Translation]](
          s"$TaxonomyApiEndpoint/resources/$resourceId/translations")
      } yield translations :+ Translation(Language.DefaultLanguage, topic.name)
    }

    /**
      * Returns sequence of names with associated language in a tuple.
      * @param topicId Id of topic to fetch.
      * @return Sequence of tuples with (name, language)
      */
    def getTopicTranslations(topicId: String): Try[Seq[Translation]] = {
      for {
        topic <- getTopic(topicId)
        translations <- get[Seq[Translation]](
          s"$TaxonomyApiEndpoint/topics/$topicId/translations")
      } yield translations :+ Translation(Language.DefaultLanguage, topic.name)
    }

    /**
      * Returns sequence of names with associated language in a tuple.
      * @param subjectId Id of subject to fetch.
      * @return Sequence of tuples with (name, language)
      */
    def getSubjectTranslations(subjectId: String): Try[Seq[Translation]] = {
      for {
        subject <- getSubject(subjectId)
        translations <- get[Seq[Translation]](
          s"$TaxonomyApiEndpoint/subjects/$subjectId/translations")
      } yield
        translations :+ Translation(Language.DefaultLanguage, subject.name)
    }

    def getTranslations(id: String): Try[Seq[Translation]] = {
      if (id.contains(":resource:")) {
        getResourceTranslations(id)
      } else if (id.contains(":topic:")) {
        getTopicTranslations(id)
      } else if (id.contains(":subject:")) {
        getSubjectTranslations(id)
      } else {
        Failure(new RuntimeException("Nope")) // TODO: find (make?) more fitting exception
      }
    }


    def resolvePath(path: String): Try[PathResolve] = {
      get[PathResolve](s"$TaxonomyApiEndpoint/url/resolve", ("path", path))
    }

    def getAllResources: Try[Seq[Resource]] =
      get[Seq[Resource]](s"$TaxonomyApiEndpoint/resources/")

    def getAllSubjects: Try[Seq[Resource]] =
      get[Seq[Resource]](s"$TaxonomyApiEndpoint/subjects/")

    def getAllTopics: Try[Seq[Resource]] =
      get[Seq[Resource]](s"$TaxonomyApiEndpoint/topics/")

    def getAllResourceTypes: Try[Seq[ResourceType]] =
      get[Seq[ResourceType]](s"$TaxonomyApiEndpoint/resource-types/")

    def getAllTopicResourceConnections: Try[Seq[TopicResourceConnection]] =
      get[Seq[TopicResourceConnection]](
        s"$TaxonomyApiEndpoint/topic-resources/")

    def getAllTopicSubtopicConnections: Try[Seq[TopicSubtopicConnection]] =
      get[Seq[TopicSubtopicConnection]](
        s"$TaxonomyApiEndpoint/topic-subtopics/")

    def getAllResourceResourceTypeConnections
      : Try[Seq[ResourceResourceTypeConnection]] =
      get[Seq[ResourceResourceTypeConnection]](
        s"$TaxonomyApiEndpoint/resource-resourcetypes/")

    def getAllSubjectTopicConnections: Try[Seq[SubjectTopicConnection]] =
      get[Seq[SubjectTopicConnection]](s"$TaxonomyApiEndpoint/subject-topics/")

    def getAllRelevances: Try[Seq[Relevance]] =
      get[Seq[Relevance]](s"$TaxonomyApiEndpoint/relevances/")

    def getAllFilters: Try[Seq[Filter]] =
      get[Seq[Filter]](s"$TaxonomyApiEndpoint/filters/")

    def getAllResourceFilterConnections: Try[Seq[ResourceFilterConnection]] =
      get[Seq[ResourceFilterConnection]](
        s"$TaxonomyApiEndpoint/resource-filters/")

    def getAllTopicFilterConnections: Try[Seq[TopicFilterConnection]] =
      get[Seq[TopicFilterConnection]](s"$TaxonomyApiEndpoint/topic-filters/")

    def getFilterConnectionsForResource(
        resourceId: String): Try[Seq[FilterConnection]] =
      get[Seq[FilterConnection]](
        s"$TaxonomyApiEndpoint/resources/$resourceId/filters")

    def getFilterConnectionsForTopic(
        topicId: String): Try[Seq[FilterConnection]] =
      get[Seq[FilterConnection]](
        s"$TaxonomyApiEndpoint/topics/$topicId/filters"
      )

    def queryResources(contentUri: String): Try[Seq[QueryResourceResult]] =
      get[Seq[QueryResourceResult]](
        s"$TaxonomyApiEndpoint/queries/resources/?contentURI=$contentUri")

    def queryTopics(contentUri: String): Try[Seq[Resource]] =
      get[Seq[Resource]](
        s"$TaxonomyApiEndpoint/queries/topics/?contentURI=$contentUri")

    def getFilter(filterId: String): Try[Filter] =
      get[Filter](s"$TaxonomyApiEndpoint/filters/$filterId")

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

    private def get[A](url: String, params: (String, String)*)(
        implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(url).params(params))
    }
  }
}
