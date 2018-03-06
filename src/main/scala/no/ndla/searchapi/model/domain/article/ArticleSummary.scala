/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

case class ArticleSummary(id: Long, title: Seq[ArticleTitle], visualElement: Seq[VisualElement], introduction: Seq[ArticleIntroduction], url: String, license: String)
