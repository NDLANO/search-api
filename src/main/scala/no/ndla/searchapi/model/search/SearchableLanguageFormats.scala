/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.util.Date
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, Extraction}
import org.json4s._

object LanguagelessSearchableArticle {

  case class LanguagelessSearchableArticle(
      id: Long,
      lastUpdated: Date,
      license: String,
      authors: Seq[String],
      articleType: String,
      defaultTitle: Option[String]
  )

  def apply(
      searchableArticle: SearchableArticle): LanguagelessSearchableArticle = {
    LanguagelessSearchableArticle(
      searchableArticle.id,
      searchableArticle.lastUpdated,
      searchableArticle.license,
      searchableArticle.authors,
      searchableArticle.articleType,
      searchableArticle.defaultTitle
    )
  }
}

class SearchableArticleSerializer
    extends CustomSerializer[SearchableArticle](_ =>
      ({
        case obj: JObject =>
          implicit val formats = org.json4s.DefaultFormats
          SearchableArticle(
            id = (obj \ "id").extract[Long],
            title = SearchableLanguageValues("title", obj),
            content = SearchableLanguageValues("content", obj),
            visualElement = SearchableLanguageValues("visualElement", obj),
            introduction = SearchableLanguageValues("introduction", obj),
            metaDescription = SearchableLanguageValues("metaDescription", obj),
            tags = SearchableLanguageList("tags", obj),
            lastUpdated = (obj \ "lastUpdated").extract[Date],
            license = (obj \ "license").extract[String],
            authors = (obj \ "authors").extract[Seq[String]],
            articleType = (obj \ "articleType").extract[String],
            defaultTitle = (obj \ "defaultTitle").extract[Option[String]]
          )
      }, {
        case article: SearchableArticle =>
          implicit val formats = org.json4s.DefaultFormats
          val languageFields =
            List(
              article.title.toJsonField("title"),
              article.content.toJsonField("content"),
              article.visualElement.toJsonField("visualElement"),
              article.introduction.toJsonField("introduction"),
              article.metaDescription.toJsonField("metaDescription"),
              article.tags.toJsonField("tags")
            ).flatMap {
              case l: Seq[JField] => l
              case _              => Seq.empty
            }
          val partialSearchableArticle = LanguagelessSearchableArticle(article)
          val partialJObject = Extraction.decompose(partialSearchableArticle)
          partialJObject.merge(JObject(languageFields: _*))
      }))

object SearchableLanguageFormats {
  val JSonFormats: Formats =
    org.json4s.DefaultFormats +
      new SearchableArticleSerializer
}
