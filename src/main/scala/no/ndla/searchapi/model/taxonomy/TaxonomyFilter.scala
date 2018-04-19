/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

import no.ndla.searchapi.model.search.SearchableLanguageValues
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JObject, JString}

case class TaxonomyFilter(filterId: String, name: SearchableLanguageValues, relevance: SearchableLanguageValues)

object SearchableContextFilters {

  def apply(name: String, jsonObject: JObject): List[TaxonomyFilter] = {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

    val filters = (jsonObject \ "filters").extract[List[JObject]]

    filters.map(filter => {
      val id = (filter \ "filterId").extract[String]
      val names = SearchableLanguageValues("name", filter)
      val relevances = SearchableLanguageValues("relevance", filter)
      TaxonomyFilter(id, names, relevances)
    })
  }
}
