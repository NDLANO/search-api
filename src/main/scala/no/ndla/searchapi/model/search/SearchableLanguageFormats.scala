/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.util.TimeZone
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.domain.{
  LanguagelessSearchableTaxonomyContext,
  SearchableTaxonomyContext
}
import no.ndla.searchapi.model.taxonomy.SearchableContextFilters
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, Extraction, _}

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
            supportedLanguages = (obj \ "supportedLanguages").extract[List[String]],
            contexts = (obj \ "contexts").extract[List[SearchableTaxonomyContext]]
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
            license = (obj \ "license").extract[api.learningpath.Copyright],
            isBasedOn = (obj \ "isBasedOn").extract[Option[Long]],
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
            filters = SearchableContextFilters("filters", obj),
            resourceTypes = SearchableLanguageList("resourceTypes", obj)
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

object SearchableLanguageFormats {
  val JSonFormats: Formats =
    org.json4s.DefaultFormats +
      new SearchableArticleSerializer +
      new TaxonomyContextSerializer +
      new SearchableLearningStepSerializer +
      new SearchableLearningPathSerializer ++
      org.json4s.ext.JodaTimeSerializers.all
}
