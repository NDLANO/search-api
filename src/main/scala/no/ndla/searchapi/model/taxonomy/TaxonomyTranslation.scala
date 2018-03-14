/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

import no.ndla.searchapi.model.domain.LanguageField

case class TaxonomyTranslation(
    language: String,
    name: String
) extends LanguageField[String] {
  override val value = name
}
