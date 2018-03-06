/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

import no.ndla.searchapi.model.domain.LanguageField

case class ArticleTag(tags: Seq[String],  language: String) extends LanguageField[Seq[String]] {
  override def value: Seq[String] = tags
}
