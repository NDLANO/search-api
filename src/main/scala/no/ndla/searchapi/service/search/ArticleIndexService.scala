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
import no.ndla.searchapi.integration.{ArticleApiClient, TaxonomyBundle}
import no.ndla.searchapi.model.domain.article.Article
import no.ndla.searchapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import org.json4s.native.Serialization.write

import scala.util.Try

trait ArticleIndexService {
  this: SearchConverterService
    with IndexService
    with ArticleApiClient =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends LazyLogging with IndexService[Article, SearchableArticle] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments("articles")
    override val searchIndex: String = SearchApiProperties.SearchIndexes("articles")
    override val apiClient: ArticleApiClient = articleApiClient

    override def createIndexRequest(domainModel: Article, indexName: String, taxonomyBundle: Option[TaxonomyBundle]): Try[IndexDefinition] = {
      val source = write(searchConverterService.asSearchableArticle(domainModel, taxonomyBundle))
      indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString)
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          longField("id"),
          keywordField("defaultTitle"),
          dateField("lastUpdated"),
          keywordField("license"),
          textField("authors").fielddata(true),
          textField("articleType").analyzer("keyword"),
          longField("metaImageId"),
          keywordField("resourceTypeIds"),
          keywordField("contentTypeIds"),
          keywordField("subjectIds")
        )
          ++
          generateLanguageSupportedFieldList("title", keepRaw = true) ++
          generateLanguageSupportedFieldList("content") ++
          generateLanguageSupportedFieldList("visualElement") ++
          generateLanguageSupportedFieldList("introduction") ++
          generateLanguageSupportedFieldList("metaDescription") ++
          generateLanguageSupportedFieldList("tags")
      )
    }
  }

}
