/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.searchapi.model.domain.LanguageField

case class Description(description: String, language: String) extends LanguageField[String] {
  override def value: String = description
}
