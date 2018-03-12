/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.search.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.mapping.ISO639
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.api.article
import no.ndla.network.ApplicationUrl
import no.ndla.searchapi.model.domain.Language.{findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.searchapi.integration.{DraftApiClient, TaxonomyBundle}
import no.ndla.searchapi.model.domain.Language
import no.ndla.searchapi.model.search._
import no.ndla.searchapi.service.ConverterService
import org.json4s.Formats
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup

import scala.util.Success

trait SearchConverterService {
  this: DraftApiClient
  with ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def getId(contentUri: String): String = {
      contentUri.split('.').lastOption match {
        case Some(cid) => cid
        case None => ""
      }
    }

    // TODO: Rename getTaxonomyStuff
    def getTaxonomyStuff(id: Long, bundle: TaxonomyBundle): (Seq[String], Seq[String], Seq[String], Seq[String]) = {
      // Get resources connected to content id
      val resources = bundle.resources.filter(resource => resource.contentUri match {
        case Some(contentUri) => getId(contentUri) == id.toString // TODO: include type in this? learningpath:123 and article:123 is not the same
        case None => false
      })

      // Get topics that contains any of the connected resources
      val resourceTopicConnections = bundle.topicResourceConnections.filter(connection => resources.map(_.id).contains(connection.resourceId))

      // Get topics connected to content id
      val topics = bundle.topics.filter(topic => topic.contentUri match {
        case Some(contentUri) => getId(contentUri) == id.toString
        case None => false
      })

      val topicIds = topics.map(_.id) ++ resourceTopicConnections.map(_.topicid)

      // Get subjects connected to the connected topics
      val subjects = bundle.subjectTopicConnections.filter(c => topicIds.contains(c.topicId)).map(_.subjectId)


      // Get relevances connected
      val relevances = bundle.relevances


      // filters, relevances, resourceTypes, subjects)
      (Seq.empty, Seq.empty, Seq.empty, subjects)
    }



    def asSearchableArticle(ai: Article, taxonomyBundle: Option[TaxonomyBundle]): SearchableArticle = {
      val (filters, relevances, resourceTypes, subjects) = getTaxonomyStuff(ai.id.get, taxonomyBundle.get)/*taxonomyBundle match {
        case Some(bundle) =>
          getTaxonomyStuff(ai.id.get, bundle)
        case None => // TODO: fetch taxonomy for single resource/topic here.
      }*/

      val articleWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = articleWithAgreement.title.sortBy(title => {
        val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
        languagePriority.indexOf(title.language)
      }).lastOption

      val supportedLanguages = Language.getSupportedLanguages(ai.title, ai.visualElement, ai.introduction, ai.metaDescription, ai.content, ai.tags)


      SearchableArticle(
        id = articleWithAgreement.id.get,
        title = SearchableLanguageValues(articleWithAgreement.title.map(title => LanguageValue(title.language, title.title))),
        visualElement = SearchableLanguageValues(articleWithAgreement.visualElement.map(visual => LanguageValue(visual.language, visual.resource))),
        introduction = SearchableLanguageValues(articleWithAgreement.introduction.map(intro => LanguageValue(intro.language, intro.introduction))),
        metaDescription = SearchableLanguageValues(articleWithAgreement.metaDescription.map(meta => LanguageValue(meta.language, meta.content))),
        content = SearchableLanguageValues(articleWithAgreement.content.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
        tags = SearchableLanguageList(articleWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = articleWithAgreement.updated,
        license = articleWithAgreement.copyright.license,
        authors = articleWithAgreement.copyright.creators.map(_.name) ++ articleWithAgreement.copyright.processors.map(_.name) ++ articleWithAgreement.copyright.rightsholders.map(_.name),
        articleType = articleWithAgreement.articleType,
        metaImageId = None,
        filters = filters,
        relevances = relevances,
        resourceTypes = resourceTypes,
        subjectIds = subjects,
        defaultTitle = defaultTitle.map(t => t.title),
        supportedLanguages = supportedLanguages
      )
    }

    def createUrlToArticle(id: Long): String = {
      s"${ApplicationUrl.get}$id"
    }

    /**
      * Attempts to extract language that hit from highlights in elasticsearch response.
      * @param result Elasticsearch hit.
      * @return Language if found.
      */
    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keyLanguages = keys.toList.flatMap(key => key.split('.').toList match {
          case _ :: language :: _ => Some(language)
          case _ => None
        })

        keyLanguages.sortBy(lang => {
          ISO639.languagePriority.reverse.indexOf(lang)
        }).lastOption
      }

      val highlightKeys: Option[Map[String, _]] = Option(result.highlight)
      val matchLanguage = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          keyToLanguage(result.sourceAsMap.keys)
      }
    }

    /**
      * Returns article summary from json string returned by elasticsearch.
      * Will always return summary, even if language does not exist in hitString.
      * Language will be prioritized according to [[findByLanguageOrBestEffort]].
      * @param hitString Json string returned from elasticsearch for one article.
      * @param language Language to extract from the hitString.
      * @return Article summary extracted from hitString in specified language.
      */
    def hitAsArticleSummary(hitString: String, language: String): ArticleSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

      val searchableArticle = read[SearchableArticle](hitString)

      val titles = searchableArticle.title.languageValues.map(lv => article.ArticleTitle(lv.value, lv.lang))
      val introductions = searchableArticle.introduction.languageValues.map(lv => article.ArticleIntroduction(lv.value, lv.lang))
      val metaDescriptions = searchableArticle.metaDescription.languageValues.map(lv => article.ArticleMetaDescription(lv.value, lv.lang))
      val visualElements = searchableArticle.visualElement.languageValues.map(lv => article.VisualElement(lv.value, lv.lang))

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(article.ArticleTitle("", Language.UnknownLanguage))
      val visualElement = findByLanguageOrBestEffort(visualElements, language)
      val introduction = findByLanguageOrBestEffort(introductions, language)
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language)

      ArticleSummary(
        searchableArticle.id,
        title,
        visualElement,
        introduction,
        metaDescription,
        ApplicationUrl.get + searchableArticle.id.toString,
        searchableArticle.license,
        searchableArticle.articleType,
        supportedLanguages
      )
    }
  }
}
