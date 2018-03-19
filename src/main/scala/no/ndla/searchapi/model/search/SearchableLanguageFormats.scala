/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.util.Date

import no.ndla.searchapi.model.domain.{LanguagelessSearchableTaxonomyContext, SearchableTaxonomyContext}
import no.ndla.searchapi.model.taxonomy.{ContextFilter, SearchableContextFilters}
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, Extraction}
import org.json4s._

class SearchableArticleSerializer
    extends CustomSerializer[SearchableArticle](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = org.json4s.DefaultFormats + new TaxonomyContextSerializer
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
            defaultTitle = (obj \ "defaultTitle").extract[Option[String]],
            metaImageId = (obj \ "metaImageId").extract[Option[Long]],
            supportedLanguages =
              (obj \ "supportedLanguages").extract[Seq[String]],
            contexts = (obj \ "contexts").extract[Seq[SearchableTaxonomyContext]]
          )
      }, {
        case article: SearchableArticle =>
          implicit val formats: Formats = org.json4s.DefaultFormats + new TaxonomyContextSerializer
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

class TaxonomyContextSerializer
    extends CustomSerializer[SearchableTaxonomyContext](_ =>
      ({
        case obj: JObject =>
          implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

          SearchableTaxonomyContext(
            id = (obj \ "id").extract[String],
            subject = SearchableLanguageValues("subject", obj),
            path = (obj \ "path").extract[String],
            breadcrumbs = SearchableLanguageList("breadcrumbs", obj),
            contextType = (obj \ "contextType").extract[String],
            filters = SearchableContextFilters("filters", obj)
          )
      }, {
        case context: SearchableTaxonomyContext =>
          implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
          val languageFields =
            List(
              context.breadcrumbs.toJsonField("breadcrumbs"),
              context.subject.toJsonField("subject")
            ).flatMap {
              case l: Seq[JField] => l
              case _              => Seq.empty
            }

          val filters = JArray(context.filters.map(f => {
            val fields = List(f.name.toJsonField("name"),
            f.relevance.toJsonField("relevance")).flatMap{
              case l: Seq[JField] => l
              case _ => Seq.empty
            }

            JObject(fields: _*)
          }))

          val languageObject = JObject(
              ("filters", filters)
              +: languageFields
          )

          val partialSearchableContext = LanguagelessSearchableTaxonomyContext(context)
          val partialJObject = Extraction.decompose(partialSearchableContext)
          partialJObject.merge(languageObject)
      }))

object SearchableLanguageFormats {
  val JSonFormats: Formats =
    org.json4s.DefaultFormats +
      new SearchableArticleSerializer +
      new TaxonomyContextSerializer
}
