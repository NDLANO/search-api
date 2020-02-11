/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search.settings

import no.ndla.searchapi.model.domain.Sort
import no.ndla.searchapi.model.domain.article.LearningResourceType

case class SearchSettings(
    query: Option[String],
    fallback: Boolean,
    language: String,
    license: Option[String],
    page: Int,
    pageSize: Int,
    sort: Sort.Value,
    withIdIn: List[Long],
    taxonomyFilters: List[String],
    subjects: List[String],
    resourceTypes: List[String],
    learningResourceTypes: List[LearningResourceType.Value],
    supportedLanguages: List[String],
    relevanceIds: List[String],
    contextIds: List[String],
    competences: List[String]
)
