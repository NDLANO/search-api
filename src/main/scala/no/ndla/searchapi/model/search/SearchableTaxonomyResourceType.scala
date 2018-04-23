/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import org.json4s.{CustomSerializer, Formats}
import org.json4s.JsonAST.{JField, JObject, JString}

case class SearchableTaxonomyResourceType(id: String, name: SearchableLanguageValues)

class SearchableTaxonomyResourceTypeSerializer
    extends CustomSerializer[SearchableTaxonomyResourceType](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          SearchableTaxonomyResourceType(
            id = (obj \ "id").extract[String],
            name = SearchableLanguageValues("name", obj)
          )
      }, {
        case resourceType: SearchableTaxonomyResourceType =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
          val languageFields: List[JField] =
            List(
              resourceType.name.toJsonField("name")
            ).flatten

          val allFields = languageFields :+ JField("id", JString(resourceType.id))
          JObject(allFields: _*)
      }))
