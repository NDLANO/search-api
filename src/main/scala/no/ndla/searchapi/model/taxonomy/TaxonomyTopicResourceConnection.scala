/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class TaxonomyTopicResourceConnection(topicid: String,
                                           resourceId: String,
                                           id: String,
                                           primary: Boolean,
                                           rank: Int)
