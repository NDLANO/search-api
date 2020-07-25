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
  val ContactEmail = "support+api@ndla.no"

  val DefaultLanguage = "nb"

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  lazy val Domain = Domains.get(Environment)

  val DraftApiUrl: String = s"http://${propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")}"
  val ArticleApiUrl: String = s"http://${propOrElse("ARTICLE_API_HOST", "article-api.ndla-local")}"
  val LearningpathApiUrl: String = s"http://${propOrElse("LEARNINGPATH_API_HOST", "learningpath-api.ndla-local")}"
  val ImageApiUrl: String = s"http://${propOrElse("IMAGE_API_HOST", "image-api.ndla-local")}"
  val AudioApiUrl: String = s"http://${propOrElse("AUDIO_API_HOST", "audio-api.ndla-local")}"
  val ApiGatewayUrl: String = s"http://${propOrElse("API_GATEWAY_HOST", "api-gateway.ndla-local")}"
  val GrepApiUrl: String = s"https://${propOrElse("GREP_API_HOST", "data.udir.no")}"

  val SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-search-api.ndla-local")
  val SearchRegion: String = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  val SearchIndexes: Map[SearchType.Value, String] = Map(
    SearchType.Articles -> propOrElse("ARTICLE_SEARCH_INDEX_NAME", "articles"),
    SearchType.Drafts -> propOrElse("DRAFT_SEARCH_INDEX_NAME", "drafts"),
    SearchType.LearningPaths -> propOrElse("LEARNINGPATH_SEARCH_INDEX_NAME", "learningpaths"),
    SearchType.Concepts -> propOrElse("CONCEPT_SEARCH_INDEX_NAME", "concepts")
  )

  val SearchDocuments: Map[SearchType.Value, String] = Map(
    SearchType.Articles -> "article",
    SearchType.Drafts -> "draft",
    SearchType.LearningPaths -> "learningpath",
    SearchType.Concepts -> "concept",
  )

  val DefaultPageSize = 10
  val MaxPageSize = 10000
  val IndexBulkSize = 100
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
