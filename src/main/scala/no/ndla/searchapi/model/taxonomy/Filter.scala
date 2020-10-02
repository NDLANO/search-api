/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class Filter(id: String, name: String, subjectId: String, metadata: Option[Metadata])
