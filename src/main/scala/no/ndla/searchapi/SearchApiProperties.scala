/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{AuthUser, Domains}
import no.ndla.searchapi.model.search.SearchType

import scala.util.Properties._

object SearchApiProperties extends LazyLogging {
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "search-api"
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val ApplicationPort: Int = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "christergundersen@ndla.no"

  val DefaultLanguage = "nb"

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  lazy val Domain = Domains.get(Environment)

  val DraftApiUrl: String = propOrElse("DRAFT_API_HOST", "http://draft-api.ndla-local")
  val ArticleApiUrl: String = propOrElse("ARTICLE_API_HOST", "http://article-api.ndla-local")
  val LearningpathApiUrl: String = propOrElse("LEARNINGPATH_API_HOST", "http://learningpath-api.ndla-local")
  val ImageApiUrl: String = propOrElse("IMAGE_API_HOST", "http://image-api.ndla-local")
  val AudioApiUrl: String = propOrElse("AUDIO_API_HOST", "http://audio-api.ndla-local")
  val ApiGatewayUrl: String = propOrElse("API_GATEWAY_HOST", "http://api-gateway.ndla-local")

  val SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-search-api.ndla-local")
  val SearchRegion: String = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  val SearchIndexes: Map[SearchType.Value, String] = Map(
    SearchType.Articles -> propOrElse("ARTICLE_SEARCH_INDEX_NAME", "articles"),
    SearchType.Drafts -> propOrElse("DRAFT_SEARCH_INDEX_NAME", "drafts"),
    SearchType.LearningPaths -> propOrElse("LEARNINGPATH_SEARCH_INDEX_NAME", "learningpaths")
  )

  val SearchDocuments: Map[SearchType.Value, String] = Map(
    SearchType.Articles -> "article",
    SearchType.Drafts -> "draft",
    SearchType.LearningPaths -> "learningpath"
  )

  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 1000
  val ElasticSearchIndexMaxResultWindow = 10000
  val ElasticSearchScrollKeepAlive = "10s"

  val ExternalApiUrls: Map[String, String] = Map(
    "article-api" -> s"$Domain/article-api/v2/articles",
    "draft-api" -> s"$Domain/draft-api/v1/drafts",
    "learningpath-api" -> s"$Domain/learningpath-api/v2/learningpaths",
    "raw-image" -> s"$Domain/image-api/raw/id"
  )

  def booleanProp(key: String) = prop(key).toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = envOrElse(key, default)
}
