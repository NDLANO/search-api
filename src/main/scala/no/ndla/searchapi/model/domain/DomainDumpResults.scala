/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import no.ndla.searchapi.model.domain.article.Article
import scala.math.ceil

sealed trait DomainDumpResult[T] {
  def totalCount: Long

  def pageSize: Int

  val numPages: Int = ceil(totalCount.toDouble / pageSize.toDouble).toInt

  val results: Seq[T]
}

case class ArticleDumpResult(totalCount: Long,
                             page: Int,
                             pageSize: Int,
                             results: Seq[Article]) extends DomainDumpResult
