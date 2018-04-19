/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import org.json4s.{CustomSerializer, DefaultFormats, Extraction, Formats}
import org.json4s.JsonAST.{JArray, JField, JObject, JString}

case class SearchableTaxonomyContext(id: String,
                                     subjectId: String,
                                     subject: SearchableLanguageValues,
                                     path: String,
                                     breadcrumbs: SearchableLanguageList,
                                     contextType: String,
                                     filters: List[SearchableTaxonomyFilter],
                                     resourceTypes: List[SearchableTaxonomyResourceType])

class SearchableTaxonomyContextSerializer
    extends CustomSerializer[SearchableTaxonomyContext](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          SearchableTaxonomyContext(
            id = (obj \ "id").extract[String],
            subjectId = (obj \ "subjectId").extract[String],
            subject = SearchableLanguageValues("subject", obj),
            path = (obj \ "path").extract[String],
            breadcrumbs = SearchableLanguageList("breadcrumbs", obj),
            contextType = (obj \ "contextType").extract[String],
            filters = (obj \ "filters").extract[List[SearchableTaxonomyFilter]],
            resourceTypes = (obj \ "resourceTypes").extract[List[SearchableTaxonomyResourceType]]
          )
      }, {
        case context: SearchableTaxonomyContext =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
          val languageFields: List[JField] =
            List(
              context.breadcrumbs.toJsonField("breadcrumbs"),
              context.subject.toJsonField("subject")
            ).flatten

          val partialSearchableContext =
            LanguagelessSearchableTaxonomyContext(context)
          val partialJObject = Extraction.decompose(partialSearchableContext)
          partialJObject.merge(JObject(languageFields: _*))
      }))

object LanguagelessSearchableTaxonomyContext {

  case class LanguagelessSearchableTaxonomyContext(id: String,
                                                   path: String,
                                                   subjectId: String,
                                                   contextType: String,
                                                   filters: List[SearchableTaxonomyFilter],
                                                   resourceTypes: List[SearchableTaxonomyResourceType])

  def apply(searchableTaxonomyContext: SearchableTaxonomyContext): LanguagelessSearchableTaxonomyContext = {
    LanguagelessSearchableTaxonomyContext(
      id = searchableTaxonomyContext.id,
      path = searchableTaxonomyContext.path,
      subjectId = searchableTaxonomyContext.subjectId,
      contextType = searchableTaxonomyContext.contextType,
      filters = searchableTaxonomyContext.filters,
      resourceTypes = searchableTaxonomyContext.resourceTypes
    )
  }
}
