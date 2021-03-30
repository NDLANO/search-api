/*
 * Part of NDLA search_api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

case class EmbedValues(
    id: Option[String],
    resource: Option[String],
    language: String
)