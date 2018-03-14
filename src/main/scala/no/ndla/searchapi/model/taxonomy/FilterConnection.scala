/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class FilterConnection(connectionId: String,
                            id: String,
                            name: String,
                            relevanceId: String)
