/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.taxonomy.{SearchableContextFilters, TaxonomyFilter}
import org.json4s.{CustomSerializer, DefaultFormats, Extraction}
import org.json4s.JsonAST.{JArray, JField, JObject, JString}

case class SearchableTaxonomyContext(id: String,
                                     subjectId: String,
                                     subject: SearchableLanguageValues,
                                     path: String,
                                     breadcrumbs: SearchableLanguageList,
                                     contextType: String,
                                     filters: List[TaxonomyFilter],
                                     resourceTypes: SearchableLanguageList,
                                     resourceTypeIds: List[String]
                                    )

class SearchableTaxonomyContextSerializer
    extends CustomSerializer[SearchableTaxonomyContext](_ =>
      ({
        case obj: JObject =>
          implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

          SearchableTaxonomyContext(
            id = (obj \ "id").extract[String],
            subjectId = (obj \ "subjectId").extract[String],
            subject = SearchableLanguageValues("subject", obj),
            path = (obj \ "path").extract[String],
            breadcrumbs = SearchableLanguageList("breadcrumbs", obj),
            contextType = (obj \ "contextType").extract[String],
            filters = SearchableContextFilters("filters", obj),
            resourceTypes = SearchableLanguageList("resourceTypes", obj),
            resourceTypeIds = (obj \ "resourceTypeIds").extract[List[String]]
          )
      }, {
        case context: SearchableTaxonomyContext =>
          implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
          val languageFields: List[JField] =
            List(
              context.breadcrumbs.toJsonField("breadcrumbs"),
              context.resourceTypes.toJsonField("resourceTypes"),
              context.subject.toJsonField("subject")
            ).flatten

          val filters = JArray(context.filters.map(f => {
            val fields: List[JField] =
              JField("filterId", JString(f.filterId)) +:
              List(f.name.toJsonField("name"),
                   f.relevance.toJsonField("relevance")).flatten

            JObject(fields: _*)
          }))

          val languageObject = JObject(
            ("filters", filters)
              +: languageFields
          )

          val partialSearchableContext =
            LanguagelessSearchableTaxonomyContext(context)
          val partialJObject = Extraction.decompose(partialSearchableContext)
          partialJObject.merge(languageObject)
      }))

object LanguagelessSearchableTaxonomyContext {

  case class LanguagelessSearchableTaxonomyContext(id: String,
                                                   path: String,
                                                   subjectId: String,
                                                   contextType: String,
                                                   resourceTypeIds: List[String])

  def apply(searchableTaxonomyContext: SearchableTaxonomyContext)
    : LanguagelessSearchableTaxonomyContext = {
    LanguagelessSearchableTaxonomyContext(
      id = searchableTaxonomyContext.id,
      path = searchableTaxonomyContext.path,
      subjectId = searchableTaxonomyContext.subjectId,
      contextType = searchableTaxonomyContext.contextType,
      resourceTypeIds = searchableTaxonomyContext.resourceTypeIds
    )
  }
}
