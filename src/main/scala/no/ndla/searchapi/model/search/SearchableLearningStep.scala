/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import org.json4s.{CustomSerializer, Formats}
import org.json4s.JsonAST.{JField, JObject, JString}

case class SearchableLearningStep(stepType: String,
                                  title: SearchableLanguageValues,
                                  description: SearchableLanguageValues)

class SearchableLearningStepSerializer
    extends CustomSerializer[SearchableLearningStep](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          SearchableLearningStep(
            stepType = (obj \ "stepType").extract[String],
            title = SearchableLanguageValues("title", obj),
            description = SearchableLanguageValues("description", obj)
          )
      }, {
        case ls: SearchableLearningStep =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
          val languageFields: List[JField] =
            List(
              ls.title.toJsonField("title"),
              ls.description.toJsonField("description")
            ).flatten

          val allFields = languageFields :+ JField("stepType",
                                                   JString(ls.stepType))
          JObject(allFields: _*)
      }))
