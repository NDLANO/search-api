/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.Domains
import no.ndla.network.secrets.Secrets.readSecrets

import scala.util.Properties._
import scala.util.{Failure, Success}

object SearchApiProperties extends LazyLogging {
  val Auth0LoginEndpoint = "https://ndla.eu.auth0.com/authorize"

  val ApplicationPort: Int = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "christergundersen@ndla.no"
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")

  val DefaultLanguage = "nb"

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  lazy val Domain = Domains.get(Environment)

  val DraftApiUrl = "http://draft-api.ndla-local"
  val ArticleApiUrl = "http://article-api.ndla-local"
  val LearningpathApiUrl = "http://learningpath-api.ndla-local"
  val ImageApiUrl = "http://image-api.ndla-local"
  val AudioApiUrl = "http://audio-api.ndla-local"

  val SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-multi.ndla-local")
  val SearchRegion: String = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val SearchIndexes: Map[String, String] = Map(
    "articles" -> propOrElse("ARTICLE_SEARCH_INDEX_NAME", "articles"),
    "concepts" -> propOrElse("CONCEPT_SEARCH_INDEX_NAME", "concepts"),
    "learningpaths" -> propOrElse("LEARNINGPATH_SEARCH_INDEX_NAME", "learningpaths")
  )

  val SearchDocuments: Map[String, String] = Map(
    "articles" -> "article",
    "concepts" -> "concept",
    "learningpaths" -> "learningpath"
  )
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 200
  val ElasticSearchIndexMaxResultWindow = 10000


  def booleanProp(key: String) = prop(key).toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = envOrElse(key, default)
}
