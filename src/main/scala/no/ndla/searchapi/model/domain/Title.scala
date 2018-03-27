/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class Title(title: String, language: String) extends LanguageField[String] {
  override def value: String = title
}
