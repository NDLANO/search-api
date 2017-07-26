/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class SearchParams(query: Option[String], language: Option[String], sort: Sort.Value, page: Int, pageSize: Int)

