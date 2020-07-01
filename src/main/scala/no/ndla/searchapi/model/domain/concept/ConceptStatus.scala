/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.concept

object ConceptStatus extends Enumeration {
  val DRAFT, PUBLISHED, QUALITY_ASSURED, QUEUED_FOR_LANGUAGE, TRANSLATED, UNPUBLISHED, ARCHIVED = Value

  def valueOf(s: String): Option[ConceptStatus.Value] = values.find(_.toString == s.toUpperCase)
}
