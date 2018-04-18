/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

import no.ndla.searchapi.model.domain.LanguageField

case class VisualElement(resource: String, language: String) extends LanguageField[String] {
  override def value: String = resource
}
