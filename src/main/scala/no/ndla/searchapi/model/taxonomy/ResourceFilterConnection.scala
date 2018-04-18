/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class ResourceFilterConnection(resourceId: String, filterId: String, id: String, relevanceId: String)
    extends FilterConnection {
  override def objectId: String = resourceId
}
