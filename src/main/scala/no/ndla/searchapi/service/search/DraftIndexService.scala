/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.DraftApiClient
import no.ndla.searchapi.model.domain.draft.Draft
import no.ndla.searchapi.model.search.{SearchableDraft, SearchableLanguageFormats}
import no.ndla.searchapi.model.taxonomy.Bundle
import no.ndla.searchapi.model.search.SearchType
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait DraftIndexService {
  this: SearchConverterService with IndexService with DraftApiClient =>
  val draftIndexService: DraftIndexService

  class DraftIndexService extends LazyLogging with IndexService[Draft] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments(SearchType.Drafts)
    override val searchIndex: String = SearchApiProperties.SearchIndexes(SearchType.Drafts)
    override val apiClient: DraftApiClient = draftApiClient

    override def createIndexRequest(domainModel: Draft,
                                    indexName: String,
                                    taxonomyBundle: Option[Bundle]): Try[IndexDefinition] = {
      searchConverterService.asSearchableDraft(domainModel, taxonomyBundle) match {
        case Success(searchableDraft) =>
          val source = write(searchableDraft)
          Success(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
        case Failure(ex) =>
          Failure(ex)
      }
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          intField("id"),
          dateField("lastUpdated"),
          keywordField("license"),
          keywordField("defaultTitle"),
          textField("authors"),
          keywordField("articleType"),
          keywordField("supportedLanguages"),
          textField("notes"),
          getTaxonomyContextMapping
        ) ++
          generateKeywordLanguageFields("metaImage") ++
          generateLanguageSupportedFieldList("title", keepRaw = true) ++
          generateLanguageSupportedFieldList("metaDescription") ++
          generateLanguageSupportedFieldList("content") ++
          generateLanguageSupportedFieldList("visualElement") ++
          generateLanguageSupportedFieldList("introduction") ++
          generateLanguageSupportedFieldList("tags")
      )
    }
  }

}
