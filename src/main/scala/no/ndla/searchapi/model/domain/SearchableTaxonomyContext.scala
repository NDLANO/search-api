/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import no.ndla.searchapi.model.search.{SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.taxonomy.ContextFilter

case class SearchableTaxonomyContext(id: String,
                                     subject: SearchableLanguageValues,
                                     path: String,
                                     breadcrumbs: SearchableLanguageList,
                                     contextType: String,
                                     filters: List[ContextFilter],
                                     resourceTypes: SearchableLanguageList)

object LanguagelessSearchableTaxonomyContext {
  case class LanguagelessSearchableTaxonomyContext(id: String,
                                                   path: String,
                                                   contextType: String)

  def apply(searchableTaxonomyContext: SearchableTaxonomyContext)
    : LanguagelessSearchableTaxonomyContext = {
    LanguagelessSearchableTaxonomyContext(
      searchableTaxonomyContext.id,
      searchableTaxonomyContext.path,
      searchableTaxonomyContext.contextType
    )
  }
}
