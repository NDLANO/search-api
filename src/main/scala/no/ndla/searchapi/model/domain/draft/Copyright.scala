/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

import no.ndla.searchapi.model.domain.Author
import org.joda.time.DateTime

case class Copyright(
                      license: Option[String],
                      origin: Option[String],
                      creators: Seq[Author],
                      processors: Seq[Author],
                      rightsholders: Seq[Author],
                      agreementId: Option[Long],
                      validFrom: Option[DateTime],
                      validTo: Option[DateTime]
                    )
