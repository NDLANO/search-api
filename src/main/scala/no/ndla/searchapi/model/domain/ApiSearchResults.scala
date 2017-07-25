/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import scala.math.ceil

trait ApiSearchResults {
  def totalCount: Long
  def pageSize: Int

  val numPages: Int = ceil(totalCount.toDouble / pageSize.toDouble).toInt
}
