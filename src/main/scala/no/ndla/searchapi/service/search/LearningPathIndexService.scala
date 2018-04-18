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
import no.ndla.searchapi.integration.LearningPathApiClient
import no.ndla.searchapi.model.domain.article.Article
import no.ndla.searchapi.model.domain.learningpath.LearningPath
import no.ndla.searchapi.model.search.{SearchType, SearchableArticle, SearchableLanguageFormats, SearchableLearningPath}
import no.ndla.searchapi.model.taxonomy.Bundle
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait LearningPathIndexService {
  this: SearchConverterService
    with IndexService
    with LearningPathApiClient =>
  val learningPathIndexService: LearningPathIndexService

  class LearningPathIndexService extends LazyLogging with IndexService[LearningPath] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments(SearchType.LearningPaths)
    override val searchIndex: String = SearchApiProperties.SearchIndexes(SearchType.LearningPaths)
    override val apiClient: LearningPathApiClient = learningPathApiClient

    override def createIndexRequest(domainModel: LearningPath, indexName: String, taxonomyBundle: Option[Bundle]): Try[IndexDefinition] = {
      searchConverterService.asSearchableLearningPath(domainModel, taxonomyBundle) match {
        case Success(searchableLearningPath) =>
          val source = write(searchableLearningPath)
          Success(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
        case Failure(ex) =>
          Failure(ex)
      }
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          intField("id"),
          textField("coverPhotoUrl"),
          intField("duration"),
          textField("status"),
          textField("verificationStatus"),
          dateField("lastUpdated"),
          keywordField("defaultTitle"),
          textField("author"),
          nestedField("learningsteps").fields(
            List(
              textField("stepType")
            ) ++
              generateLanguageSupportedFieldList("title") ++
              generateLanguageSupportedFieldList("description")
          ),
          objectField("copyright").fields(
            objectField("license").fields(
              textField("license"),
              textField("description"), textField("url")
            ),
            nestedField("contributors").fields(
              textField("type"),
              textField("name")
            )
          ),
          intField("isBasedOn"),
          keywordField("supportedLanguages"),
          getTaxonomyContextMapping
        ) ++
          generateLanguageSupportedFieldList("title", keepRaw = true) ++
          generateLanguageSupportedFieldList("description") ++
          generateLanguageSupportedFieldList("tags", keepRaw = true)
      )
    }
  }

}
