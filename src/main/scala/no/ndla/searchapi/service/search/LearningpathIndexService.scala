/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */


package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.ArticleApiClient
import no.ndla.searchapi.model.domain.article.Article
import no.ndla.searchapi.model.domain.learningpath.LearningPath
import no.ndla.searchapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.searchapi.model.taxonomy.Bundle
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait LearningpathIndexService {
  this: SearchConverterService
    with IndexService
    with ArticleApiClient =>
  val learningpathIndexService: LearningpathIndexService

  class LearningpathIndexService extends LazyLogging with IndexService[LearningPath, SearchableLearningpath] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments("learningpaths")
    override val searchIndex: String = SearchApiProperties.SearchIndexes("learningpaths")
    override val apiClient: ArticleApiClient = articleApiClient

    override def createIndexRequest(domainModel: LearningPath, indexName: String, taxonomyBundle: Option[Bundle]): Try[IndexDefinition] = ???

    def getMapping: MappingDefinition = {
        intField("id"),
        generateLanguageSupportedFieldList("titles", keepRaw = true),
        generateLanguageSupportedFieldList("descriptions"),
        textField("coverPhotoUrl"),
        intField("duration"),
        textField("status"),
        textField("verificationStatus"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        generateLanguageSupportedFieldList("tags", keepRaw = true),
        textField("author"),
        nestedField("learningsteps").fields(
          List(
            textField("stepType")
          ) ++
          generateLanguageSupportedFieldList("titles") ++
          generateLanguageSupportedFieldList("descriptions")
        ),
        objectField("copyright").fields(
          objectField("license").fields(
            textField("license"),
            textField("description"),
            textField("url")
          ),
          nestedField("contributors").fields(
            textField("type"),
            textField("name")
          )
        ),
        intField("isBasedOn"),
      keywordField("supportedLanguages"),
      nestedField("contexts").fields(
        List(
          keywordField("id"),
          keywordField("path"),
          keywordField("contextType")
        ) ++
          generateLanguageSupportedFieldList("resourceTypes", keepRaw = true) ++
          generateLanguageSupportedFieldList("subject", keepRaw = true) ++
          generateLanguageSupportedFieldList("breadcrumbs") ++
          List(nestedField("filters").fields(
            generateLanguageSupportedFieldList("name", keepRaw = true) ++
              generateLanguageSupportedFieldList("relevance")
          ))
      )

    }
  }

}
