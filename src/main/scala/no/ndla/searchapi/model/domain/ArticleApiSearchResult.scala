/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain


case class ArticleApiSearchResult(id: Long,
                                  title: String,
                                  visualElement: String,
                                  introduction: String,
                                  url: String,
                                  license: String,
                                  articleType: String,
                                  supportedLanguages: Seq[String])
