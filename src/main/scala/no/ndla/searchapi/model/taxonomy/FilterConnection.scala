/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

trait FilterConnection {
  def objectId: String

  def filterId: String

  def id: String

  def relevanceId: String
}
