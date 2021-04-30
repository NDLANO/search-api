/*
 * Part of NDLA search_api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

object ArticleTrait extends Enumeration {
  val VIDEO: ArticleTrait.Value = Value("VIDEO")
  val H5P: ArticleTrait.Value = Value("H5P")
  val PODCAST: ArticleTrait.Value = Value("PODCAST")

  def all: List[String] = ArticleTrait.values.map(_.toString).toList
}
