/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.concept

case class Status(current: ConceptStatus.Value, other: Set[ConceptStatus.Value])
