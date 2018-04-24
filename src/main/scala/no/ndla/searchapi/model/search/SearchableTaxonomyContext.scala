/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

case class SearchableTaxonomyContext(id: String,
                                     subjectId: String,
                                     subject: SearchableLanguageValues,
                                     path: String,
                                     breadcrumbs: SearchableLanguageList,
                                     contextType: String,
                                     filters: List[SearchableTaxonomyFilter],
                                     resourceTypes: List[SearchableTaxonomyResourceType])

