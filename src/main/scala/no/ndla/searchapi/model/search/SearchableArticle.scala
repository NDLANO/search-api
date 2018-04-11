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

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: DateTime,
    license: String,
    authors: List[String],
    articleType: String,
    metaImageId: Option[String],
    defaultTitle: Option[String],
    supportedLanguages: List[String],
    contexts: List[SearchableTaxonomyContext]
)

class SearchableArticleSerializer
    extends CustomSerializer[SearchableArticle](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          val time = (obj \ "lastUpdated").extract[DateTime]
          val tz = TimeZone.getDefault
          val lastUpdated = new DateTime(time, DateTimeZone.forID(tz.getID))

          SearchableArticle(
            id = (obj \ "id").extract[Long],
            title = SearchableLanguageValues("title", obj),
            content = SearchableLanguageValues("content", obj),
            visualElement = SearchableLanguageValues("visualElement", obj),
            introduction = SearchableLanguageValues("introduction", obj),
            metaDescription = SearchableLanguageValues("metaDescription", obj),
            tags = SearchableLanguageList("tags", obj),
            lastUpdated = lastUpdated,
            license = (obj \ "license").extract[String],
            authors = (obj \ "authors").extract[List[String]],
            articleType = (obj \ "articleType").extract[String],
            defaultTitle = (obj \ "defaultTitle").extract[Option[String]],
            metaImageId = (obj \ "metaImageId").extract[Option[String]],
            supportedLanguages =
              (obj \ "supportedLanguages").extract[List[String]],
            contexts =
              (obj \ "contexts").extract[List[SearchableTaxonomyContext]]
          )
      }, {
        case article: SearchableArticle =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
          val languageFields: List[JField] =
            List(
              article.title.toJsonField("title"),
              article.content.toJsonField("content"),
              article.visualElement.toJsonField("visualElement"),
              article.introduction.toJsonField("introduction"),
              article.metaDescription.toJsonField("metaDescription"),
              article.tags.toJsonField("tags")
            ).flatten

          val partialSearchableArticle = LanguagelessSearchableArticle(article)
          val partialJObject = Extraction.decompose(partialSearchableArticle)
          partialJObject.merge(JObject(languageFields: _*))
      }))

object LanguagelessSearchableArticle {
  case class LanguagelessSearchableArticle(
      id: Long,
      lastUpdated: DateTime,
      license: String,
      authors: List[String],
      articleType: String,
      metaImageId: Option[String],
      defaultTitle: Option[String],
      supportedLanguages: List[String],
      contexts: List[SearchableTaxonomyContext]
  )

  def apply(
      searchableArticle: SearchableArticle): LanguagelessSearchableArticle = {
    LanguagelessSearchableArticle(
      searchableArticle.id,
      searchableArticle.lastUpdated,
      searchableArticle.license,
      searchableArticle.authors,
      searchableArticle.articleType,
      searchableArticle.metaImageId,
      searchableArticle.defaultTitle,
      searchableArticle.supportedLanguages,
      searchableArticle.contexts
    )
  }
}
