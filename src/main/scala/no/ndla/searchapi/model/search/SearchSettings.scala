/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.domain.Sort
import no.ndla.searchapi.model.domain.article.LearningResourceType

case class SearchSettings(fallback: Boolean,
                          language: String,
                          license: Option[String],
                          page: Int,
                          pageSize: Int,
                          sort: Sort.Value,
                          withIdIn: List[Long],
                          taxonomyFilters: List[String],
                          subjects: List[String],
                          resourceTypes: List[String],
                          contextTypes: List[LearningResourceType.Value],
                          supportedLanguages: List[String])
