/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import org.json4s.{CustomSerializer, Formats}
import org.json4s.JsonAST.{JField, JObject, JString}

case class SearchableTaxonomyFilter(filterId: String,
                                    name: SearchableLanguageValues,
                                    relevance: SearchableLanguageValues)

class SearchableTaxonomyFilterSerializer
    extends CustomSerializer[SearchableTaxonomyFilter](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          SearchableTaxonomyFilter(
            filterId = (obj \ "filterId").extract[String],
            name = SearchableLanguageValues("name", obj),
            relevance = SearchableLanguageValues("relevance", obj)
          )
      }, {
        case filter: SearchableTaxonomyFilter =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
          val languageFields: List[JField] =
            List(
              filter.name.toJsonField("name"),
              filter.relevance.toJsonField("relevance")
            ).flatten

          val allFields = languageFields :+ JField("filterId", JString(filter.filterId))
          val obby = JObject(allFields: _*)
          obby
      }))
