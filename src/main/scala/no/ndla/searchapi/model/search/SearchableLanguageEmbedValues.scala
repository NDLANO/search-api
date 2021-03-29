/*
 * Part of NDLA search_api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

case class EmbedValues(
    id: String,
    resource: String,
)

case class SearchableLanguageEmbedValues(languageValues: Seq[LanguageValue[Seq[EmbedValues]]])
