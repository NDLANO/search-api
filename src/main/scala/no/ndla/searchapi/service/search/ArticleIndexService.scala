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
import no.ndla.searchapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.searchapi.model.taxonomy.Bundle
import org.json4s.native.Serialization.write
import no.ndla.searchapi.model.search.SearchType

import scala.util.{Failure, Success, Try}

trait ArticleIndexService {
  this: SearchConverterService with IndexService with ArticleApiClient =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends LazyLogging with IndexService[Article] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments(SearchType.Articles)
    override val searchIndex: String = SearchApiProperties.SearchIndexes(SearchType.Articles)
    override val apiClient: ArticleApiClient = articleApiClient

    override def createIndexRequest(domainModel: Article,
                                    indexName: String,
                                    taxonomyBundle: Option[Bundle]): Try[IndexDefinition] = {
      searchConverterService.asSearchableArticle(domainModel, taxonomyBundle) match {
        case Success(searchableArticle) =>
          val source = write(searchableArticle)
          Success(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
        case Failure(ex) =>
          Failure(ex)
      }
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          longField("id"),
          keywordField("defaultTitle"),
          dateField("lastUpdated"),
          keywordField("license"),
          textField("authors"),
          keywordField("articleType"),
          keywordField("supportedLanguages"),
          getTaxonomyContextMapping
        )
          ++
            generateKeywordLanguageFields("metaImage") ++
          generateLanguageSupportedFieldList("title", keepRaw = true) ++
          generateLanguageSupportedFieldList("metaDescription") ++
          generateLanguageSupportedFieldList("content") ++
          generateLanguageSupportedFieldList("visualElement") ++
          generateLanguageSupportedFieldList("introduction") ++
          generateLanguageSupportedFieldList("metaDescription") ++
          generateLanguageSupportedFieldList("tags")
      )
    }
  }

}
