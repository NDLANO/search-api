/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

trait WithLanguage {
  def language: String
}

trait LanguageField[T] extends WithLanguage {
  def value: T
  def language: String
}
