/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class TopicFilterConnection(topicId: String,
                                 filterId: String,
                                 id: String,
                                 relevanceId: String)
