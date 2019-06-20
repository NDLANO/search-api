/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class TopicResourceTypeConnection(
    topicId: String,
    resourceTypeId: String,
    id: String
) extends ResourceTypeConnection
