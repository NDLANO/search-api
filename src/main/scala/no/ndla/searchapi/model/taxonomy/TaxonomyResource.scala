/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class TaxonomyResource(id: String,
                            name: String,
                            contentUri: Option[String],
                            path: String)
