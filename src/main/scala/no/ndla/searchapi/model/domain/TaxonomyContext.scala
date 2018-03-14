/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class TaxonomyContext(
    id: String,
    filterId: String,
    relevanceIds: Seq[String],
    resourceTypes: Seq[String],
    subjectId: String
)
