/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import no.ndla.searchapi.model.taxonomy.{ContextFilter, Translation}

case class TaxonomyContext(id: String,
                          subjectName: List[Translation],
                          path: String,
                          breadcrumbs: List[List[Translation]],
                          contextType: String,
                          filters: List[ContextFilter])
