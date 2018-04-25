/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.domain.LanguageField

case class LanguageValue[T](language: String, value: T) extends LanguageField[T]

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

object SearchableLanguageValues {

  def fieldsToSearchableLanguageValues[T <: LanguageField[String]](fields: Seq[T]): SearchableLanguageValues = {
    SearchableLanguageValues(fields.map(f => LanguageValue(f.language, f.value)))
  }
}

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])
