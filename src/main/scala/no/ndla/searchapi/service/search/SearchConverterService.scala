/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import cats.instances.duration
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
import no.ndla.searchapi.model.domain.learningpath.{LearningPath, LearningStep, StepType}
import no.ndla.searchapi.model.{api, domain, taxonomy}
import no.ndla.searchapi.model.domain.{Language, LanguageField, SearchableTaxonomyContext}
import no.ndla.searchapi.model.search._
import no.ndla.searchapi.model.taxonomy.{ContextFilter, _}
import no.ndla.searchapi.service.ConverterService
import org.json4s.{DefaultFormats, Formats, ShortTypeHints, TypeHints}
import org.json4s.native.Serialization.read
import no.ndla.mapping.License.getLicense
import no.ndla.searchapi.model.api.learningpath.LearningPathSummary
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

trait SearchConverterService {
  this: DraftApiClient
    with TaxonomyApiClient
    with ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def getParentTopicsAndPaths(topic: Resource, bundle: Bundle, path: List[String]): List[(Resource, List[String])] = {
      val parentConnections = bundle.topicSubtopicConnections.filter(_.subtopicid == topic.id)
      val parents = bundle.topics.filter(t => parentConnections.map(_.topicid).contains(t.id))

      parents.flatMap(parent => getParentTopicsAndPaths(parent, bundle, path :+ parent.id)) :+ (topic, path)
    }

    def asSearchableArticle(ai: Article, taxonomyBundle: Option[Bundle]): Try[SearchableArticle] = {
      val taxonomyForArticle = taxonomyBundle match {
        case Some(bundle) => getTaxonomyContexts(ai.id.get, "article", bundle)
        case None =>
          taxonomyApiClient.getTaxonomyBundle match {
            case Success(bundle) =>
              getTaxonomyContexts(ai.id.get, "article", bundle)
            case Failure(ex) =>
              logger.error("Could not fetch bundle from taxonomy...")
              Failure(ex)
          }
      }

      taxonomyForArticle match {
        case Success(contexts) =>
          if (contexts.isEmpty) {
            Failure(ElasticIndexingException(s"No taxonomy found for article with id '${ai.id.getOrElse(-1)}'"))
          } else {
            val articleWithAgreement = converterService.withAgreementCopyright(ai)

            val defaultTitle = articleWithAgreement.title.sortBy(title => {
              ISO639.languagePriority.reverse.indexOf(title.language)
            }).lastOption

            val supportedLanguages = Language.getSupportedLanguages(ai.title, ai.visualElement, ai.introduction, ai.metaDescription, ai.content, ai.tags).toList

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
              authors = (articleWithAgreement.copyright.creators.map(_.name) ++ articleWithAgreement.copyright.processors.map(_.name) ++ articleWithAgreement.copyright.rightsholders.map(_.name)).toList,
              articleType = articleWithAgreement.articleType,
              metaImageId = None, //TODO: get metaImageId // Maybe fetch it on way out and remove it from SearchableArticle?
              defaultTitle = defaultTitle.map(t => t.title),
              supportedLanguages = supportedLanguages,
              contexts = contexts
            ))
          }
        case Failure(ex) => Failure(ex)
      }

    }

    def asSearchableLearningPath(lp: LearningPath, taxonomyBundle: Option[Bundle]): Try[SearchableLearningPath] = {
      val taxonomyForLearningPath = taxonomyBundle match {
        case Some(bundle) => getTaxonomyContexts(lp.id.get, "learningpath", bundle)
        case None =>
          taxonomyApiClient.getTaxonomyBundle match {
            case Success(bundle) =>
              getTaxonomyContexts(lp.id.get, "learningpath", bundle)
            case Failure(ex) =>
              logger.error("Could not fetch bundle from taxonomy...")
              Failure(ex)
          }
      }

      taxonomyForLearningPath match {
        case Success(contexts) =>
          if (contexts.isEmpty) {
            Failure(ElasticIndexingException(s"No taxonomy found for learningpath with id '${lp.id.getOrElse(-1)}'"))
          } else {
            val defaultTitle = lp.title.sortBy(title =>
              ISO639.languagePriority.reverse.indexOf(title.language)
            ).lastOption

            val license = api.learningpath.Copyright(asLearningPathApiLicense(lp.copyright.license),
              lp.copyright.contributors.map(c => api.learningpath.Author(c.`type`, c.name)))

            Success(SearchableLearningPath(
              id = lp.id.get,
              title = SearchableLanguageValues.fieldsToSearchableLanguageValues(lp.title),
              description = SearchableLanguageValues.fieldsToSearchableLanguageValues(lp.description),
              coverPhotoUrl = None, // TODO: Fetch coverPhotoUrl
              duration = lp.duration,
              status = lp.status.toString,
              verificationStatus = lp.verificationStatus.toString,
              lastUpdated = lp.lastUpdated,
              defaultTitle = defaultTitle.map(_.title),
              tags = SearchableLanguageList(lp.tags.map(tag => LanguageValue(tag.language, tag.tags))),
              learningsteps = lp.learningsteps.map(asSearchableLearningStep),
              license = license,
              isBasedOn = lp.isBasedOn
            ))
          }
        case Failure(ex) => Failure(ex)
      }
    }

    def asLearningPathApiLicense(license: String): api.learningpath.License = {
      getLicense(license) match {
        case Some(l) => api.learningpath.License(l.license, Option(l.description), l.url)
        case None => api.learningpath.License(license, Some("Invalid license"), None)
      }
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      val nonHtmlDescriptions = learningStep.description.map(desc => domain.learningpath.Description(Jsoup.parseBodyFragment(desc.description).text(), desc.language))
      SearchableLearningStep(
        learningStep.`type`.toString,
        SearchableLanguageValues.fieldsToSearchableLanguageValues(learningStep.title),
        SearchableLanguageValues.fieldsToSearchableLanguageValues(nonHtmlDescriptions))
    }

    /**
      * Attempts to extract language that hit from highlights in elasticsearch response.
      *
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
      *
      * @param hitString Json string returned from elasticsearch for one article.
      * @param language  Language to extract from the hitString.
      * @return Article summary extracted from hitString in specified language.
      */
    def hitAsArticleSummary(hitString: String, language: String): ArticleSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

      val searchableArticle = read[SearchableArticle](hitString)

      val titles = searchableArticle.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions = searchableArticle.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions = searchableArticle.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements = searchableArticle.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))

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

    def hitAsLearningPathSummary(hitString: String, language: String): LearningPathSummary = {
      val searchableLearningPath = read[SearchableLearningPath](hitString)

      val titles = searchableLearningPath.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val descriptions = searchableLearningPath.description.languageValues.map(lv => api.learningpath.Description(lv.value, lv.language))
      val introductionStep = searchableLearningPath.learningsteps.find(_.stepType == StepType.INTRODUCTION.toString)
      val introductions = asApiLearningPathIntroduction(introductionStep)
      val tags = searchableLearningPath.tags.languageValues.map(lv => api.learningpath.LearningPathTags(lv.value, lv.language))

      val title = findByLanguageOrBestEffort(titles, language)
      val description = findByLanguageOrBestEffort(descriptions, language)
      val introduction = findByLanguageOrBestEffort(introductions, language)
      val tag = findByLanguageOrBestEffort(tag, language)

      val supportedLanguages = getSupportedLanguages(titles, descriptions, introductions, tags)

      LearningPathSummary(
        searchableLearningPath.id,
        title,
        description,
        introduction,
        createUrlToLearningPath(searchableLearningPath.id),
        searchableLearningPath.coverPhotoUrl,
        searchableLearningPath.duration,
        searchableLearningPath.status,
        searchableLearningPath.lastUpdated,
        tag,
        searchableLearningPath.license,
        supportedLanguages,
        searchableLearningPath.isBasedOn
      )

    }

    def asApiLearningPathIntroduction(learningStep: Option[SearchableLearningStep]): List[api.learningpath.Introduction] = {
      learningStep.map(_.description) match {
        case Some(desc) => desc.languageValues.map(lv => api.learningpath.Introduction(lv.value, lv.language)).toList
        case None => List.empty
      }
    }

    def articleHitAsMultiSummary(hitString: String, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableArticle = read[SearchableArticle](hitString)

      val contexts = searchableArticle.contexts.map(c => searchableContextToApiContext(c, language))


      val titles = searchableArticle.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions = searchableArticle.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions = searchableArticle.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements = searchableArticle.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(api.MetaDescription("", Language.UnknownLanguage))

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

      val url = s"${SearchApiProperties.ExternalApiUrls("article-api")}/${searchableArticle.id}"
      val metaImageUrl = searchableArticle.metaImageId.map(id => s"${SearchApiProperties.ExternalApiUrls("raw-image")}/$id")

      MultiSearchSummary(
        id = searchableArticle.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImageUrl,
        url = url,
        contexts = contexts,
        supportedLanguages = supportedLanguages
      )

    }

    def searchableContextToApiContext(context: SearchableTaxonomyContext, language: String): ApiTaxonomyContext = {
      val subjectName = findByLanguageOrBestEffort(context.subject.languageValues, language).map(_.value).getOrElse("")
      val breadcrumbs = findByLanguageOrBestEffort(context.breadcrumbs.languageValues, language).map(_.value).getOrElse(Seq.empty).toList

      val filters = context.filters.map(filter => taxonomyFilterToApiFilter(filter, language))

      ApiTaxonomyContext(
        id = context.id,
        subject = subjectName,
        path = context.path,
        breadcrumbs = breadcrumbs,
        filters = filters,
        learningResourceType = context.contextType.toString,
        language = language
      )

    }

    def taxonomyFilterToApiFilter(filter: taxonomy.ContextFilter, language: String): api.ContextFilter = {
      val name = findByLanguageOrBestEffort(filter.name.languageValues, language).map(_.value).getOrElse("")
      val relevance = findByLanguageOrBestEffort(filter.relevance.languageValues, language).map(_.value).getOrElse("")

      api.ContextFilter(
        name,
        relevance
      )
    }

    private def compareId(contentUri: String, id: Long, `type`: String): Boolean = {
      val split = contentUri.split(':')
      split match {
        case Array(_, cType: String, cId: String) => id.toString == cId && cType == `type`
        case _ => false
      }
    }

    private def getContextType(resourceId: String, contentUri: Option[String]): Try[LearningResourceType.Value] = {
      contentUri match {
        case Some(uri) if uri.contains("article") =>
          if (resourceId.contains(":topic:")) {
            Success(LearningResourceType.TopicArticle)
          } else {
            Success(LearningResourceType.Standard)
          }
        case Some(uri) if uri.contains("learningpath") => Success(LearningResourceType.LearningPath)
        case _ =>
          val msg = s"Could not find type for resource $resourceId"
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
      }
    }

    private def getBreadcrumbFromIds(ids: List[String], bundle: Bundle): Seq[String] = {
      ids.map(id => {
        bundle.getObject(id).map(_.name).getOrElse("")
      })
    }

    /**
      * Returns filters connected to an object and a subject.
      *
      * @param resource Taxonomy Object (Can be resource or topic)
      * @param subject Filters must be connected to subject
      * @param bundle Bundle for resolving taxonomy
      * @param objectFilterConnections [[ResourceFilterConnection]]'s or [[TopicFilterConnection]]'s
      * @return
      */
    private def getFilters(resource: Resource, subject: Resource, bundle: Bundle, objectFilterConnections: List[FilterConnection]): List[ContextFilter] = {
      val subjectFilters = bundle.filters.filter(_.subjectId == subject.id)
      val filterConnections = objectFilterConnections
        .filter(_.objectId == resource.id)
        .filter(fc => subjectFilters.map(_.id).contains(fc.filterId))

      val connectedFilters = filterConnections.map(fc =>
        (bundle.filters.find(_.id == fc.filterId), fc)
      )

      connectedFilters.flatMap({
        case (Some(filter), filterConnection) =>
          val relevanceName = bundle.relevances
            .find(r => r.id == filterConnection.relevanceId).map(_.name).getOrElse("")

          Some(taxonomy.ContextFilter(
            name = SearchableLanguageValues(Seq(LanguageValue(Language.DefaultLanguage, filter.name))), // TODO: Get translations
            relevance = SearchableLanguageValues(Seq(LanguageValue(Language.DefaultLanguage, relevanceName))) // TODO: Get translations
          ))
        case _ => None
      })
    }

    /**
      * Returns every parent of resourceType.
      * @param resourceType ResourceType to derive parents for.
      * @param allTypes All resourceTypes to derive parents from.
      * @return List of parents including resourceType.
      */
    def getResourceTypeParents(resourceType: ResourceType, allTypes: List[ResourceType]): List[ResourceType] = {
      def allTypesWithParents(allTypes: List[ResourceType], parents: List[ResourceType]): List[(ResourceType, List[ResourceType])] = {
        allTypes.flatMap(resourceType => {
          val thisLevelWithParents = allTypes.map(resourceType =>
            (resourceType, parents)
          )

          val nextLevelWithParents = resourceType.subtypes match {
            case Some(subtypes) => allTypesWithParents(subtypes, parents :+ resourceType)
            case None => List.empty
          }
          nextLevelWithParents ++ thisLevelWithParents
        })
      }
      allTypesWithParents(allTypes, List.empty).filter(x => x._1 == resourceType).flatMap(_._2).distinct
    }

    /**
      * Returns a flattened list of resourceType with its subtypes
      * @param resourceType A resource with subtypes
      * @return Flattened list of resourceType with subtypes.
      */
    private def getTypeAndSubtypes(resourceType: ResourceType): List[ResourceType] = {
      def getTypeAndSubtypesWithParent(resourceType: ResourceType, parents: List[ResourceType] = List.empty): List[ResourceType] = {
        resourceType.subtypes match {
          case None => (parents :+ resourceType).distinct
          case Some(subtypes) =>
            subtypes.flatMap(x =>
              getTypeAndSubtypesWithParent(x, parents :+ resourceType)
            )
        }
      }
      getTypeAndSubtypesWithParent(resourceType, List.empty)
    }

    private def getResourceTaxonomyContexts(resource: Resource, taxonomyType: String, bundle: Bundle): Try[List[SearchableTaxonomyContext]] = {
      val topicsConnections = bundle.topicResourceConnections.filter(_.resourceId == resource.id)
      val topics = bundle.topics.filter(topic => topicsConnections.map(_.topicid).contains(topic.id))
      val parentTopicsAndPaths = topics.flatMap(t => getParentTopicsAndPaths(t, bundle, List(t.id)))

      val resourceTypeConnections = bundle.resourceResourceTypeConnections.filter(_.resourceId == resource.id)
      val allResourceTypes = bundle.resourceTypes.flatMap(rt => getTypeAndSubtypes(rt))

      // Every explicitly specified resourceType
      val resourceTypes = allResourceTypes.filter(r => resourceTypeConnections.map(_.resourceTypeId).contains(r.id))

      // Include parents of resourceTypes if they exist
      val subParents = resourceTypes.flatMap(rt => getResourceTypeParents(rt, bundle.resourceTypes)).filterNot(resourceTypes.contains)
      val resourceTypesWithParents = (resourceTypes ++ subParents).distinct

      val searchableResourceTypes = SearchableLanguageList(Seq(LanguageValue(Language.DefaultLanguage, resourceTypesWithParents.map(_.name)))) // TODO: Get translations

      getContextType(resource.id, resource.contentUri) match {
        case Success(contextType) =>
          val contexts = parentTopicsAndPaths.map({
            case (topic, topicPath) =>
              val subjectConnections = bundle.subjectTopicConnections.filter(_.topicid == topic.id)
              val subjects = bundle.subjects.filter(subject => subjectConnections.map(_.subjectid).contains(subject.id))

              subjects.map(subject => {
                val contextFilters = getFilters(resource, subject, bundle, bundle.resourceFilterConnections)
                val pathIds = (resource.id +: topicPath :+ subject.id).reverse

                getSearchableTaxonomyContext(resource.id, pathIds, subject.name, contextType, contextFilters, searchableResourceTypes, bundle)
              })
          })
          Success(contexts.flatten)
        case Failure(ex) => Failure(ex)
      }
    }

    private def getSearchableTaxonomyContext(taxonomyId: String,
                                             pathIds: List[String],
                                             subjectName: String,
                                             contextType: LearningResourceType.Value,
                                             contextFilters: List[ContextFilter],
                                             resourceTypes: SearchableLanguageList,
                                             bundle: Bundle) = {

      val path = "/" + pathIds.map(_.replace("urn:", "")).mkString("/")

      val subjectLanguageValues = SearchableLanguageValues(Seq(LanguageValue(Language.DefaultLanguage, subjectName))) // TODO: Get translations
      val breadcrumbList = Seq(LanguageValue(Language.DefaultLanguage, getBreadcrumbFromIds(pathIds.dropRight(1), bundle))) // TODO: Get translations
      val breadcrumbs = SearchableLanguageList(breadcrumbList)

      SearchableTaxonomyContext(
        id = taxonomyId,
        subject = subjectLanguageValues,
        path = path,
        contextType = contextType.toString,
        breadcrumbs = breadcrumbs,
        filters = contextFilters,
        resourceTypes = resourceTypes
      )
    }

    private def getTopicTaxonomyContexts(topic: Resource, taxonomyType: String, bundle: Bundle): Try[List[SearchableTaxonomyContext]] = {
      val topicsConnections = bundle.topicResourceConnections.filter(_.resourceId == topic.id)
      val topics = bundle.topics.filter(topic => topicsConnections.map(_.topicid).contains(topic.id)) :+ topic
      val parentTopicsAndPaths = topics.flatMap(t => getParentTopicsAndPaths(t, bundle, List(t.id)))

      getContextType(topic.id, topic.contentUri) match {
        case Success(contextType) =>
          val contexts = parentTopicsAndPaths.map({
            case (parentTopic, topicPath) =>
              val subjectConnections = bundle.subjectTopicConnections.filter(_.topicid == parentTopic.id)
              val subjects = bundle.subjects.filter(subject => subjectConnections.map(_.subjectid).contains(subject.id))

              subjects.map(subject => {
                val contextFilters = getFilters(topic, subject, bundle, bundle.topicFilterConnections)
                val pathIds = (topicPath :+ subject.id).reverse

                getSearchableTaxonomyContext(topic.id, pathIds, subject.name, contextType, contextFilters, SearchableLanguageList(Seq.empty), bundle)
              })
          })
          Success(contexts.flatten)
        case Failure(ex) => Failure(ex)
      }

    }

    /**
      * Parses [[Bundle]] to get taxonomy for a single resource/topic.
      *
      * @param id           of article/learningpath
      * @param taxonomyType Type of resource used in contentUri.
      *                     Example: "learningpath" in "urn:learningpath:123"
      * @param bundle       All taxonomy in an object.
      * @return Taxonomy that is to be indexed.
      */
    private[service] def getTaxonomyContexts(id: Long, taxonomyType: String, bundle: Bundle): Try[List[SearchableTaxonomyContext]] = {
      getTaxonomyResourceAndTopicsForId(id, bundle, taxonomyType) match {
        case (Nil, Nil) =>
          val msg = s"$id could not be found in taxonomy."
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
        case (Seq(resource), Nil) =>
          getResourceTaxonomyContexts(resource, taxonomyType, bundle)
        case (Nil, Seq(topic)) =>
          getTopicTaxonomyContexts(topic, taxonomyType, bundle)
        case (r, t) =>
          val taxonomyEntries = r ++ t
          val msg = s"$id is specified in taxonomy ${taxonomyEntries.size} times."
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
      }
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
  }

}
