/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.util.TimeZone
import no.ndla.searchapi.model.api.learningpath.Copyright
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.{CustomSerializer, Extraction, Formats}
import org.json4s.JsonAST.{JField, JObject}

case class SearchableLearningPath(id: Long,
                                  title: SearchableLanguageValues,
                                  description: SearchableLanguageValues,
                                  coverPhotoId: Option[String],
                                  duration: Option[Int],
                                  status: String,
                                  verificationStatus: String,
                                  lastUpdated: DateTime,
                                  defaultTitle: Option[String],
                                  tags: SearchableLanguageList,
                                  learningsteps: List[SearchableLearningStep],
                                  license: Copyright,
                                  isBasedOn: Option[Long],
                                  supportedLanguages: List[String],
                                  contexts: List[SearchableTaxonomyContext])

class SearchableLearningPathSerializer
    extends CustomSerializer[SearchableLearningPath](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          val time = (obj \ "lastUpdated").extract[DateTime]
          val tz = TimeZone.getDefault
          val lastUpdated = new DateTime(time, DateTimeZone.forID(tz.getID))

          SearchableLearningPath(
            id = (obj \ "id").extract[Long],
            title = SearchableLanguageValues("title", obj),
            description = SearchableLanguageValues("description", obj),
            coverPhotoId = (obj \ "coverPhotoUrl").extract[Option[String]],
            duration = (obj \ "duration").extract[Option[Int]],
            status = (obj \ "status").extract[String],
            verificationStatus = (obj \ "verificationStatus").extract[String],
            lastUpdated = lastUpdated,
            defaultTitle = (obj \ "defaultTitle").extract[Option[String]],
            tags = SearchableLanguageList("tags", obj),
            learningsteps = (obj \ "learningsteps").extract[List[SearchableLearningStep]],
            license = (obj \ "license").extract[Copyright],
            isBasedOn = (obj \ "isBasedOn").extract[Option[Long]],
            supportedLanguages = (obj \ "supportedLanguages").extract[List[String]],
            contexts = (obj \ "contexts").extract[List[SearchableTaxonomyContext]]
          )
      }, {
        case lp: SearchableLearningPath =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          val languageFields: List[JField] =
            List(
              lp.title.toJsonField("title"),
              lp.description.toJsonField("description"),
              lp.tags.toJsonField("tags")
            ).flatten

          val partialSearchableLearningPath =
            LanguagelessSearchableLearningPath(lp)
          val partialJObject =
            Extraction.decompose(partialSearchableLearningPath)
          partialJObject.merge(JObject(languageFields: _*))

      }))

object LanguagelessSearchableLearningPath {
  case class LanguagelessSearchableLearningPath(id: Long,
                                                coverPhotoUrl: Option[String],
                                                duration: Option[Int],
                                                status: String,
                                                verificationStatus: String,
                                                lastUpdated: DateTime,
                                                defaultTitle: Option[String],
                                                learningsteps: List[SearchableLearningStep],
                                                license: Copyright,
                                                isBasedOn: Option[Long],
                                                supportedLanguages: List[String],
                                                contexts: List[SearchableTaxonomyContext])

  def apply(searchableLearningPath: SearchableLearningPath): LanguagelessSearchableLearningPath = {
    LanguagelessSearchableLearningPath(
      searchableLearningPath.id,
      searchableLearningPath.coverPhotoId,
      searchableLearningPath.duration,
      searchableLearningPath.status,
      searchableLearningPath.verificationStatus,
      searchableLearningPath.lastUpdated,
      searchableLearningPath.defaultTitle,
      searchableLearningPath.learningsteps,
      searchableLearningPath.license,
      searchableLearningPath.isBasedOn,
      searchableLearningPath.supportedLanguages,
      searchableLearningPath.contexts
    )
  }
}
