/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings._
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.ArticleApiClient
import no.ndla.searchapi.model.domain.article.Article
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.search.{SearchType, SearchableLanguageFormats}
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle
import no.ndla.searchapi.repository.{ArticleRepository, Repository}
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait ArticleIndexService {
  this: SearchConverterService with IndexService with ArticleApiClient with ArticleRepository =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends LazyLogging with IndexService[Article] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments(SearchType.Articles)
    override val searchIndex: String = SearchApiProperties.SearchIndexes(SearchType.Articles)
    override val apiClient: ArticleApiClient = articleApiClient
    override val repository: Repository[Article] = articleRepository

    override def createIndexRequest(domainModel: Article,
                                    indexName: String,
                                    taxonomyBundle: TaxonomyBundle,
                                    grepBundle: GrepBundle): Try[IndexRequest] = {
      searchConverterService.asSearchableArticle(domainModel, taxonomyBundle, grepBundle) match {
        case Success(searchableArticle) =>
          val source = write(searchableArticle)
          Success(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
        case Failure(ex) =>
          Failure(ex)
      }
    }

    def getMapping: MappingDefinition = {
      mapping(documentType)
        .dynamic(DynamicMapping.Strict)
        .fields(
          List(
            longField("id"),
            keywordField("defaultTitle"),
            dateField("lastUpdated"),
            keywordField("license"),
            textField("authors"),
            keywordField("articleType"),
            keywordField("supportedLanguages"),
            keywordField("grepContexts.code"),
            textField("grepContexts.title"),
            keywordField("traits"),
            getTaxonomyContextMapping,
            nestedField("embedResourcesAndIds").fields(
              keywordField("resource"),
              keywordField("id"),
              keywordField("language")
            ),
            nestedField("metaImage").fields(
              keywordField("imageId"),
              keywordField("altText"),
              keywordField("language")
            ),
          )
            ++
              generateLanguageSupportedFieldList("title", keepRaw = true) ++
            generateLanguageSupportedFieldList("metaDescription") ++
            generateLanguageSupportedFieldList("content") ++
            generateLanguageSupportedFieldList("visualElement") ++
            generateLanguageSupportedFieldList("introduction") ++
            generateLanguageSupportedFieldList("metaDescription") ++
            generateLanguageSupportedFieldList("tags") ++
            generateLanguageSupportedFieldList("embedAttributes") ++
            // To be removed
            generateLanguageSupportedFieldList("embedResources", keepRaw = true) ++
            // To be removed
            generateLanguageSupportedFieldList("embedIds", keepRaw = true)
        )
    }
  }

}
