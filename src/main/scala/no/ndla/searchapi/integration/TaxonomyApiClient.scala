/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.searchapi.SearchApiProperties.ApiGatewayUrl
import no.ndla.network.NdlaClient
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxononyApiClient: TaxonomyApiClient

  class TaxonomyApiClient {
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

    def getAllTopicResourceConnections: Try[Seq[TaxonomyTopicResourceConnection]] =
      get[Seq[TaxonomyTopicResourceConnection]](s"$TaxonomyApiEndpoint/topic-resources/")

    def getAllTopicSubtopicConnections: Try[Seq[TaxonomyTopicSubtopicConnection]] =
      get[Seq[TaxonomyTopicSubtopicConnection]](s"$TaxonomyApiEndpoint/topic-subtopics/")

    def getTaxonomyBundle: Try[TaxonomyBundle] = {
      for {
        subjects <- getAllSubjects
        topics <- getAllTopics
        resources <- getAllResources
        topicResourceConnections <- getAllTopicResourceConnections
        topicSubtopicConnections <- getAllTopicSubtopicConnections

        bundle <- TaxonomyBundle(
          subjects,
          topics,
          resources,
          topicResourceConnections,
          topicSubtopicConnections
        )
      } yield bundle
    }

    //TODO: Probably need this when converting Hit to MultiSummary
    def queryResource(articleId: Long) =
      get(s"$TaxonomyApiEndpoint/queries/resources?contentURI=urn:article:$articleId")

    private def get[A](url: String, params: (String, String)*)(
        implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(url).params(params))
    }
  }
}

case class TaxonomyBundle(subjects: Seq[TaxonomyResource],
                          topics: Seq[TaxonomyResource],
                          resources: Seq[TaxonomyResource],
                          topicResourceConnections: Seq[TaxonomyTopicResourceConnection],
                          topicSubtopicConnections: Seq[TaxonomyTopicSubtopicConnection])

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

case class TaxonomyResource(id: String,
                            name: String,
                            contentUri: Option[String],
                            path: String)
