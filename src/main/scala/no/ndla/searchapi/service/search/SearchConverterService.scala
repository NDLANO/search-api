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
import no.ndla.searchapi.model.api.{ElasticIndexingException, article}
import no.ndla.network.ApplicationUrl
import no.ndla.searchapi.model.domain.Language.{findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.searchapi.integration.{DraftApiClient, TaxonomyBundle, TaxonomyResource}
import no.ndla.searchapi.model.domain.{Language, TaxonomyContext}
import no.ndla.searchapi.model.search._
import no.ndla.searchapi.service.ConverterService
import org.json4s.Formats
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

trait SearchConverterService {
  this: DraftApiClient
  with ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    private def compareId(contentUri: String, id: Long, `type`: String): Boolean = {
      val split = contentUri.split(':')
      split match {
        case Array(_, cType: String, cId: String) => id.toString == cId && cType == `type`
        case _ => false
      }
    }


    // TODO: Rename getTaxonomyStuff
    // TODO: include type in this? learningpath:123 and article:123 is not the same
    // TODO: Get taxonomy for single article/learningpath
    def getTaxonomyStuff(id: Long, taxonomyType: String): Try[Seq[TaxonomyContext]] = ???


    // TODO: Rename getTaxonomyStuff
    /**
      * Parses [[TaxonomyBundle]] to get taxonomy for a single resource/topic.
      *
      * @param id of article
      * @param bundle bundle of all taxonomy
      * @return Taxonomy that is to be indexed.
      */
    def getTaxonomyStuff(id: Long, bundle: TaxonomyBundle, taxonomyType: String): Try[Seq[TaxonomyContext]] = {
      getTaxonomyResourceAndTopicsForId(id, bundle, taxonomyType) match {
        case (Nil, Nil) =>
          val msg = s"$id could not be found in taxonomy."
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
        case (Seq(resource), Nil) =>
          val filterConnections = bundle.resourceFilterConnections.filter(_.resourceId == resource.id)
          val filters = bundle.filters
            .filter(f => filterConnections.map(_.filterId).contains(f.id))

          val contexts = filters.map(filter => {
            val relevanceIds = filterConnections.filter(_.filterId == filter.id).map(_.relevanceId)
            val resourceTypes = bundle.resourceResourceTypeConnections.filter(_.resourceId == resource.id).map(_.resourceTypeId)

            TaxonomyContext(
              filterId = filter.id,
              relevanceIds = relevanceIds,
              resourceTypes = resourceTypes,
              subjectId = filter.subjectId
            )
          })
          Success(contexts)
        case (Nil, Seq(topic)) =>

          val filterConnections = bundle.topicFilterConnections.filter(_.topicId == topic.id)
          val filters = bundle.filters
            .filter(f => filterConnections.map(_.filterId).contains(f.id))

          val contexts = filters.map(filter => {
            val relevanceIds = filterConnections.filter(_.filterId == filter.id).map(_.relevanceId)

            TaxonomyContext(
              filterId = filter.id,
              relevanceIds = relevanceIds,
              resourceTypes = Seq.empty, // Topics do not have resourceTypes
              subjectId = filter.subjectId
            )
          })

          Success(contexts)
        case (r, t) =>
          val taxonomyEntries = r ++ t
          val msg = s"$id is specified in taxonomy ${taxonomyEntries.size} times."
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
      }
    }

    private def getTaxonomyResourceAndTopicsForId(id: Long, bundle: TaxonomyBundle, taxonomyType: String) = {
      val resources = bundle.resources.filter(resource => resource.contentUri match {
        case Some(contentUri) => compareId(contentUri, id, taxonomyType)
        case None => false
      }).distinct

      val topics = bundle.topics.filter(topic => topic.contentUri match {
        case Some(contentUri) => compareId(contentUri, id, taxonomyType)
        case None => false
      }).distinct

      (resources, topics)
    }

    def asSearchableArticle(ai: Article, taxonomyBundle: Option[TaxonomyBundle]): Try[SearchableArticle] = {
      val taxonomyForArticle = taxonomyBundle match {
        case Some(bundle) => getTaxonomyStuff(ai.id.get, bundle, "article")
        case None => getTaxonomyStuff(ai.id.get, "article")
      }
      taxonomyForArticle match {
        case Success(contexts) =>
          val articleWithAgreement = converterService.withAgreementCopyright(ai)

          val defaultTitle = articleWithAgreement.title.sortBy(title => {
            val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
            languagePriority.indexOf(title.language)
          }).lastOption

          val supportedLanguages = Language.getSupportedLanguages(ai.title, ai.visualElement, ai.introduction, ai.metaDescription, ai.content, ai.tags)

          Success(SearchableArticle(
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
            metaImageId = None, //TODO: get metaImageId
            defaultTitle = defaultTitle.map(t => t.title),
            supportedLanguages = supportedLanguages,
            contexts = contexts
          ))
        case Failure(ex) => Failure(ex)
      }

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
