/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.indexes.IndexRequest
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.model.domain.concept.Concept
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.search.{SearchType, SearchableLanguageFormats}
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle
import org.json4s.Formats

import scala.util.{Failure, Success, Try}

trait ConceptIndexService {
  this: SearchConverterService with IndexService with ConceptApiClient =>
  val conceptIndexService: ConceptIndexService

  class ConceptIndexService extends LazyLogging with IndexService[Concept] {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments(SearchType.Drafts)
    override val searchIndex: String = SearchApiProperties.SearchIndexes(SearchType.Drafts)
    override val apiClient: ConceptApiClient = conceptApiClient

    override def createIndexRequest(domainModel: Concept,
                                    indexName: String,
                                    taxonomyBundle: TaxonomyBundle,
                                    grepBundle: GrepBundle): Try[IndexRequest] = ???

  }

}
