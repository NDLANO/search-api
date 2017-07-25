/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class ArticleApiSearchResults(totalCount: Long,
                            page: Int,
                            pageSize: Int,
                            language: String,
                            results: Seq[ArticleApiSearchResult]) extends ApiSearchResults

case class ArticleApiSearchResult(id: Long,
                                  title: String,
                                  visualElement: String,
                                  introduction: String,
                                  url: String,
                                  license: String,
                                  articleType: String,
                                  supportedLanguages: Seq[String])
