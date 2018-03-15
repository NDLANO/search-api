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
import no.ndla.searchapi.model.api._
import no.ndla.network.ApplicationUrl
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.model.domain.Language.{findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.domain.{Language, TaxonomyContext}
import no.ndla.searchapi.model.search._
import no.ndla.searchapi.model.taxonomy.{Bundle, QueryResourceResult, Resource}
import no.ndla.searchapi.service.ConverterService
import org.json4s.{DefaultFormats, Formats, ShortTypeHints, TypeHints}
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

trait SearchConverterService {
  this: DraftApiClient
  with TaxonomyApiClient
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

    private def getContextType(resourceId: String, contentUri: Option[String]): Try[String] = {
      contentUri match {
        case Some(uri) if uri.contains("article") =>
          if (resourceId.contains(":topic:")) {
            Success("topic-article")
          } else {
            Success("article")
          }
        case Some(uri) if uri.contains("learningpath") => Success("learningpath")
        case _ =>
          val msg = s"Could not find type for resource $resourceId"
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
      }
    }

    private def fetchTaxonomyResourcesAndTopicsForId(contentUri: String, taxonomyType: String): Try[(Seq[QueryResourceResult], Seq[Resource])] = {
      (taxonomyApiClient.queryResources(contentUri),
        taxonomyApiClient.queryTopics(contentUri)) match {
        case (Success(r), Success(t)) => Success((r,t))
        case (r, t) =>
          r match {
            case Failure(ex) =>
              logger.error(s"Failed to fetch resource with contentUri: '$contentUri', got message: ${ex.getMessage}")
            case _ =>
          }

          t match {
            case Failure(ex) =>
              logger.error(s"Failed to fetch topic with contentUri: '$contentUri', got message: ${ex.getMessage}")
            case _ =>
          }
          Failure(ElasticIndexingException(s"Could not query taxonomy for contentUri: '$contentUri'"))
      }
    }

    /**
      * Returns Sequence of [[TaxonomyContext]] for a single resource/topic.
      * @param id of article/learningpath
      * @param taxonomyType Type of resource used in contentUri.
      *                     Example: "learningpath" in "urn:learningpath:123"
      * @return Taxonomy that is to be indexed.
      */
    private def getTaxonomyContexts(id: Long, taxonomyType: String): Try[Seq[TaxonomyContext]] = {
      val contentUri = s"urn:$taxonomyType:$id"

      fetchTaxonomyResourcesAndTopicsForId(contentUri, taxonomyType) match {
        case Success((Seq(resource), Seq())) =>
          taxonomyApiClient.getFilterConnectionsForResource(resource.id) match {
            case Success(filterConnections) =>

              val contexts = filterConnections.map(filterCon => {
                val relevanceId = filterCon.relevanceId
                val resourceTypes = resource.resourceTypes

                taxonomyApiClient.getFilter(filterCon.id) match {
                  case Success(filter) =>
                    getContextType(resource.id, Some(resource.contentUri)) match {
                      case Success(contextType) =>
                        Success(TaxonomyContext(
                          id = resource.id,
                          filterId = filter.id,
                          relevanceId = relevanceId,
                          resourceTypes = resourceTypes.map(_.id),
                          subjectId = filter.subjectId,
                          typeInContext = contextType
                        ))
                      case Failure(ex) => Failure(ex)
                    }
                  case Failure(ex) => Failure(ex)
                }
              })
              Try(contexts.map(_.get)) // Seq[Try[_]] to Try[Seq[_]]
            case Failure(ex) =>
              logger.error(s"Fetching filterConnections for resource ${resource.id} failed...")
              Failure(ex)
          }
        case Success((Seq(), Seq(topic))) =>
          taxonomyApiClient.getFilterConnectionsForTopic(topic.id) match {
            case Success(filterConnections) =>
              val contexts = filterConnections.map(filterCon => {
                val relevanceId = filterCon.relevanceId

                taxonomyApiClient.getFilter(filterCon.id) match {
                  case Success(filter) =>
                    getContextType(topic.id, topic.contentUri) match {
                      case Success(contextType) =>
                        Success(TaxonomyContext(
                          id = topic.id,
                          filterId = filter.id,
                          relevanceId = relevanceId,
                          resourceTypes = Seq.empty,
                          subjectId = filter.subjectId,
                          typeInContext = contextType

                        ))
                      case Failure(ex) => Failure(ex)
                    }
                  case Failure(ex) => Failure(ex)
                }
              })
              Try(contexts.map(_.get))
            case Failure(ex) =>
              logger.error(s"Fetching filterConnections for topic ${topic.id} failed...")
              Failure(ex)
          }
        case Success((r, t)) =>
          val taxonomyEntries = r ++ t
          val msg = s"$id is specified in taxonomy ${taxonomyEntries.size} times."
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
        case Failure(ex) => Failure(ex)
      }
    }


    /**
      * Parses [[Bundle]] to get taxonomy for a single resource/topic.
      *
      * @param id of article/learningpath
      * @param taxonomyType Type of resource used in contentUri.
      *                     Example: "learningpath" in "urn:learningpath:123"
      * @param bundle All taxonomy in an object.
      * @return Taxonomy that is to be indexed.
      */
    private def getTaxonomyContexts(id: Long, taxonomyType: String, bundle: Bundle): Try[Seq[TaxonomyContext]] = {
      getTaxonomyResourceAndTopicsForId(id, bundle, taxonomyType) match {
        case (Nil, Nil) =>
          val msg = s"$id could not be found in taxonomy."
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
        case (Seq(resource), Nil) =>
          val filterConnections = bundle.resourceFilterConnections.filter(_.resourceId == resource.id)
          val filters = bundle.filters
            .filter(f => filterConnections.map(_.filterId).contains(f.id))

          getContextType(resource.id, resource.contentUri) match {
            case Success(contextType) =>
              val contexts = filters.map(filter => {
                val relId = filterConnections.find(_.filterId == filter.id).map(_.relevanceId).getOrElse("")
                val resourceTypes = bundle.resourceResourceTypeConnections.filter(_.resourceId == resource.id).map(_.resourceTypeId)

                TaxonomyContext(
                  id = resource.id,
                  filterId = filter.id,
                  relevanceId = relId,
                  resourceTypes = resourceTypes,
                  subjectId = filter.subjectId,
                  typeInContext = contextType
                )
              })
              Success(contexts)
            case Failure(ex) => Failure(ex)
          }
        case (Nil, Seq(topic)) =>

          val filterConnections = bundle.topicFilterConnections.filter(_.topicId == topic.id)
          val filters = bundle.filters
            .filter(f => filterConnections.map(_.filterId).contains(f.id))

          getContextType(topic.id, topic.contentUri) match {
            case Success(contextType) =>
              val contexts = filters.map(filter => {
                val relevanceId = filterConnections.find(_.filterId == filter.id).map(_.relevanceId).getOrElse("")

                TaxonomyContext(
                  id = topic.id,
                  filterId = filter.id,
                  relevanceId = relevanceId,
                  resourceTypes = Seq.empty,
                  subjectId = filter.subjectId,
                  typeInContext = contextType
                )
              })
              Success(contexts)
            case Failure(ex) => Failure(ex)
          }
        case (r, t) =>
          val taxonomyEntries = r ++ t
          val msg = s"$id is specified in taxonomy ${taxonomyEntries.size} times."
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
      }
    }

    private def getTaxonomyResourceAndTopicsForId(id: Long, bundle: Bundle, taxonomyType: String) = {
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

    def asSearchableArticle(ai: Article, taxonomyBundle: Option[Bundle]): Try[SearchableArticle] = {
      val taxonomyForArticle = taxonomyBundle match {
        case Some(bundle) => getTaxonomyContexts(ai.id.get, "article", bundle)
        case None => getTaxonomyContexts(ai.id.get, "article")
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
            metaImageId = None, //TODO: get metaImageId // On second thought maybe just on way out and remove it from SearchableArticle?
            defaultTitle = defaultTitle.map(t => t.title),
            supportedLanguages = supportedLanguages,
            contexts = contexts
          ))
        case Failure(ex) => Failure(ex)
      }

    }

    /**
      * Attempts to extract language that hit from highlights in elasticsearch response.
      * @param result Elasticsearch hit.
      * @return Language if found.
      */
    def getLanguageFromHit(result: SearchHit): Option[String] = {
      // TODO: Check if this is good enough for all types.
      // TODO: Maybe do something like if any of the splits are in supportedLanguages that is a language? TEST IT
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

      val titles = searchableArticle.title.languageValues.map(lv => Title(lv.value, lv.lang))
      val introductions = searchableArticle.introduction.languageValues.map(lv => article.ArticleIntroduction(lv.value, lv.lang))
      val metaDescriptions = searchableArticle.metaDescription.languageValues.map(lv => MetaDescription(lv.value, lv.lang))
      val visualElements = searchableArticle.visualElement.languageValues.map(lv => article.VisualElement(lv.value, lv.lang))

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
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

    def searchableContextToApiContext(context: TaxonomyContext, language: String): Try[ApiTaxonomyContext] = {
      for {
        subjectNames <- taxonomyApiClient.getTranslations(context.subjectId)

        resource <- if (context.id.contains("topic")) taxonomyApiClient.getTopic(context.id)
        else taxonomyApiClient.getResource(context.id)

        crumbs <- taxonomyApiClient.getBreadcrumbs(resource.path, language)

        relevance <- taxonomyApiClient.getRelevanceById(context.relevanceId)
      } yield ApiTaxonomyContext(
        findByLanguageOrBestEffort(subjectNames, language).map(_.name).getOrElse(""),
        resource.path,
        crumbs,
        relevance.name,
        context.typeInContext,
        language
      )
    }

    // TODO: implement this
    /*
    def learningpathHitAsMultiSummary(hitString: String, language: String): Try[MultiSearchSummary] = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableLearningPath = read[SearchableLearningPath](hitString)

      val titles = searchableLearningPath.titles.languageValues.map(lv => Title(lv.value, lv.lang))
      val metaDescriptions = searchableLearningPath.descriptions.languageValues.map(lv => MetaDescription(lv.value, lv.lang))

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(api.MetaDescription("", Language.UnknownLanguage))

      // TODO: finish learningpath to multisummary
    }*/


    def articleHitAsMultiSummary(hitString: String, language: String): Try[MultiSearchSummary] = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableArticle = read[SearchableArticle](hitString)
      val start = System.currentTimeMillis()
      val contextTries = searchableArticle.contexts.map(c => searchableContextToApiContext(c, language))
      logger.info(s"Fetching taxonomy for hit ${searchableArticle.id} took: ${System.currentTimeMillis() - start} ms...")

      Try(contextTries.map(_.get)) match {
        case Success(contexts) =>
          val titles = searchableArticle.title.languageValues.map(lv => Title(lv.value, lv.lang))
          val introductions = searchableArticle.introduction.languageValues.map(lv => article.ArticleIntroduction(lv.value, lv.lang))
          val metaDescriptions = searchableArticle.metaDescription.languageValues.map(lv => MetaDescription(lv.value, lv.lang))
          val visualElements = searchableArticle.visualElement.languageValues.map(lv => article.VisualElement(lv.value, lv.lang))

          val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
          val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(api.MetaDescription("", Language.UnknownLanguage))

          val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

          val url = s"${SearchApiProperties.ExternalApiUrls("article-api")}/${searchableArticle.id}"
          val metaImageUrl = searchableArticle.metaImageId.map(id => s"${SearchApiProperties.ExternalApiUrls("raw-image")}/$id")

          Success(MultiSearchSummary(
            id = searchableArticle.id,
            title = title,
            metaDescription = metaDescription,
            metaImage = metaImageUrl,
            url = url,
            contexts = contexts,
            supportedLanguages = supportedLanguages
          ))
        case Failure(ex) => Failure(ex)
      }

    }
  }
}
