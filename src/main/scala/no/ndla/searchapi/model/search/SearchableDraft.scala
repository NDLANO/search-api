/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.util.TimeZone
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.{CustomSerializer, Extraction, Formats}
import org.json4s.JsonAST.{JField, JObject}

case class SearchableDraft(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: DateTime,
    license: Option[String],
    authors: List[String],
    articleType: String,
    metaImage: SearchableLanguageValues,
    defaultTitle: Option[String],
    supportedLanguages: List[String],
    notes: List[String],
    contexts: List[SearchableTaxonomyContext]
)

class SearchableDraftSerializer
    extends CustomSerializer[SearchableDraft](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          val time = (obj \ "lastUpdated").extract[DateTime]
          val tz = TimeZone.getDefault
          val lastUpdated = new DateTime(time, DateTimeZone.forID(tz.getID))

          SearchableDraft(
            id = (obj \ "id").extract[Long],
            title = SearchableLanguageValues("title", obj),
            content = SearchableLanguageValues("content", obj),
            visualElement = SearchableLanguageValues("visualElement", obj),
            introduction = SearchableLanguageValues("introduction", obj),
            metaDescription = SearchableLanguageValues("metaDescription", obj),
            tags = SearchableLanguageList("tags", obj),
            lastUpdated = lastUpdated,
            license = (obj \ "license").extract[Option[String]],
            authors = (obj \ "authors").extract[List[String]],
            articleType = (obj \ "articleType").extract[String],
            defaultTitle = (obj \ "defaultTitle").extract[Option[String]],
            metaImage = SearchableLanguageValues("metaImage", obj),
            supportedLanguages = (obj \ "supportedLanguages").extract[List[String]],
            notes = (obj \ "notes").extract[List[String]],
            contexts = (obj \ "contexts").extract[List[SearchableTaxonomyContext]]
          )
      }, {
        case draft: SearchableDraft =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
          val languageFields: List[JField] =
            List(
              draft.title.toJsonField("title"),
              draft.content.toJsonField("content"),
              draft.visualElement.toJsonField("visualElement"),
              draft.introduction.toJsonField("introduction"),
              draft.metaDescription.toJsonField("metaDescription"),
              draft.tags.toJsonField("tags"),
              draft.metaImage.toJsonField("metaImage")
            ).flatten

          val partialSearchableDraft = LanguagelessSearchableDraft(draft)
          val partialJObject = Extraction.decompose(partialSearchableDraft)
          partialJObject.merge(JObject(languageFields: _*))
      }))

object LanguagelessSearchableDraft {
  case class LanguagelessSearchableDraft(
      id: Long,
      lastUpdated: DateTime,
      license: Option[String],
      authors: List[String],
      articleType: String,
      defaultTitle: Option[String],
      supportedLanguages: List[String],
      notes: List[String],
      contexts: List[SearchableTaxonomyContext]
  )

  def apply(
      draft: SearchableDraft): LanguagelessSearchableDraft = {
    LanguagelessSearchableDraft(
      id = draft.id,
      lastUpdated = draft.lastUpdated,
      license = draft.license,
      authors = draft.authors,
      articleType = draft.articleType,
      defaultTitle = draft.defaultTitle,
      supportedLanguages = draft.supportedLanguages,
      notes = draft.notes,
      contexts = draft.contexts
    )
  }
}
