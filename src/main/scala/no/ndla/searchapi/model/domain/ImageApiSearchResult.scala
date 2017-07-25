/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain


case class ImageApiSearchResult(id: Long,
                                previewUrl: String,
                                metaUrl: String,
                                license: String)
