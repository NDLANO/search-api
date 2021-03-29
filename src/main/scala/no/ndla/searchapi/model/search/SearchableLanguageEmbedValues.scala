/*
 * Part of NDLA search_api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

case class EmbedValues(
    ids: Seq[String],
    resource: Seq[String],
)

case class SearchableLanguageEmbedValues(languageValues: Seq[LanguageValue[Seq[EmbedValues]]])
