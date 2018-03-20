/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.domain.Sort

case class SearchSettings(
    fallback: Boolean,
    language: String,
    license: Option[String],
    page: Int,
    pageSize: Int,
    sort: Sort.Value,
    types: List[String],
    withIdIn: List[Long]
)
