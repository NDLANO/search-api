/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.util.Date
import no.ndla.searchapi.model.domain.SearchableTaxonomyContext

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: Date,
    license: String,
    authors: Seq[String],
    articleType: String,
    metaImageId: Option[Long],
    defaultTitle: Option[String],
    supportedLanguages: Seq[String],
    contexts: Seq[SearchableTaxonomyContext]
) extends Searchable

object LanguagelessSearchableArticle {
  case class LanguagelessSearchableArticle(
      id: Long,
      lastUpdated: Date,
      license: String,
      authors: Seq[String],
      articleType: String,
      metaImageId: Option[Long],
      defaultTitle: Option[String],
      supportedLanguages: Seq[String],
      contexts: Seq[SearchableTaxonomyContext]
  )

  def apply(
      searchableArticle: SearchableArticle): LanguagelessSearchableArticle = {
    LanguagelessSearchableArticle(
      searchableArticle.id,
      searchableArticle.lastUpdated,
      searchableArticle.license,
      searchableArticle.authors,
      searchableArticle.articleType,
      searchableArticle.metaImageId,
      searchableArticle.defaultTitle,
      searchableArticle.supportedLanguages,
      searchableArticle.contexts
    )
  }
}
