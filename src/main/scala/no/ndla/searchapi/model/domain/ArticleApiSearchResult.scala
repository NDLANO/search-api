/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain


case class ArticleApiTitle(title: String, language: String)
case class ArticleApiVisualElment(visualElement: String, language: String)
case class ArticleApiIntro(introduction: String, language: String)
case class ArticleApiSearchResult(id: Long,
                                  title: ArticleApiTitle,
                                  visualElement: Option[ArticleApiVisualElment],
                                  introduction: Option[ArticleApiIntro],
                                  url: String,
                                  license: String,
                                  articleType: String,
                                  supportedLanguages: Seq[String])
