/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import org.joda.time.DateTime

case class Copyright(
    license: Option[String],
    origin: Option[String],
    creators: List[Author],
    processors: List[Author],
    rightsholders: List[Author],
    agreementId: Option[Long],
    validFrom: Option[DateTime],
    validTo: Option[DateTime]
)
