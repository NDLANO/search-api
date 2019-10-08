/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.{DraftApiClient, TaxonomyApiClient}
import no.ndla.searchapi.model.domain.draft.Draft
import no.ndla.searchapi.model.search.{SearchType, SearchableLanguageFormats}
import no.ndla.searchapi.model.taxonomy.Bundle
import org.json4s.Formats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait DraftIndexService {
  this: SearchConverterService with IndexService with DraftApiClient with TaxonomyApiClient =>
  val draftIndexService: DraftIndexService

  class DraftIndexService extends LazyLogging with IndexService[Draft] {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments(SearchType.Drafts)
    override val searchIndex: String = SearchApiProperties.SearchIndexes(SearchType.Drafts)
    override val apiClient: DraftApiClient = draftApiClient

    override def createIndexRequest(domainModel: Draft,
                                    indexName: String,
                                    taxonomyBundle: Bundle): Try[IndexRequest] = {
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
          keywordField("draftStatus"),
          dateField("lastUpdated"),
          keywordField("license"),
          keywordField("defaultTitle"),
          textField("authors"),
          keywordField("articleType"),
          keywordField("supportedLanguages"),
          textField("notes"),
          textField("previousVersionsNotes"),
          keywordField("users"),
          getTaxonomyContextMapping,
          nestedField("metaImage").fields(
            keywordField("imageId"),
            keywordField("altText"),
            keywordField("language")
          )
        ) ++
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
