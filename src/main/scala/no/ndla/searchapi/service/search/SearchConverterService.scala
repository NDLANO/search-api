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
import no.ndla.mapping.License.getLicense
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.{article, _}
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.api.draft.DraftSummary
import no.ndla.searchapi.model.api.learningpath.LearningPathSummary
import no.ndla.searchapi.model.domain.Language.{findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.learningpath.{LearningPath, LearningStep, StepType}
import no.ndla.searchapi.model.domain.Language
import no.ndla.searchapi.model.domain.draft.Draft
import no.ndla.searchapi.model.search.{settings, _}
import no.ndla.searchapi.model.taxonomy._
import no.ndla.searchapi.model.{api, domain, search, taxonomy}
import no.ndla.searchapi.service.ConverterService
import org.json4s.Formats
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

trait SearchConverterService {
  this: DraftApiClient with TaxonomyApiClient with ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def getParentTopicsAndPaths(topic: Resource, bundle: Bundle, path: List[String]): List[(Resource, List[String])] = {
      val parentConnections = bundle.topicSubtopicConnections.filter(_.subtopicid == topic.id)
      val parents = bundle.topics.filter(t => parentConnections.map(_.topicid).contains(t.id))

      parents.flatMap(parent => getParentTopicsAndPaths(parent, bundle, path :+ parent.id)) :+ (topic, path)
    }

    def asSearchableArticle(ai: Article, taxonomyBundle: Bundle): Try[SearchableArticle] = {
      val taxonomyForArticle = getTaxonomyContexts(ai.id.get, "article", taxonomyBundle)

      val articleWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = articleWithAgreement.title
        .sortBy(title => {
          ISO639.languagePriority.reverse.indexOf(title.language)
        })
        .lastOption

      val supportedLanguages = Language
        .getSupportedLanguages(ai.title, ai.visualElement, ai.introduction, ai.metaDescription, ai.content, ai.tags)
        .toList

      Success(
        SearchableArticle(
          id = articleWithAgreement.id.get,
          title = SearchableLanguageValues(articleWithAgreement.title.map(title =>
            LanguageValue(title.language, title.title))),
          visualElement = SearchableLanguageValues(articleWithAgreement.visualElement.map(visual =>
            LanguageValue(visual.language, visual.resource))),
          introduction = SearchableLanguageValues(articleWithAgreement.introduction.map(intro =>
            LanguageValue(intro.language, intro.introduction))),
          metaDescription = SearchableLanguageValues(articleWithAgreement.metaDescription.map(meta =>
            LanguageValue(meta.language, meta.content))),
          content = SearchableLanguageValues(articleWithAgreement.content.map(article =>
            LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
          tags = SearchableLanguageList(articleWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          lastUpdated = articleWithAgreement.updated,
          license = articleWithAgreement.copyright.license,
          authors = (articleWithAgreement.copyright.creators.map(_.name) ++ articleWithAgreement.copyright.processors
            .map(_.name) ++ articleWithAgreement.copyright.rightsholders.map(_.name)).toList,
          articleType = articleWithAgreement.articleType.toString,
          metaImage = articleWithAgreement.metaImage.toList,
          defaultTitle = defaultTitle.map(t => t.title),
          supportedLanguages = supportedLanguages,
          contexts = taxonomyForArticle.getOrElse(List.empty)
        ))

    }

    def asSearchableLearningPath(lp: LearningPath, taxonomyBundle: Bundle): Try[SearchableLearningPath] = {
      val taxonomyForLearningPath = getTaxonomyContexts(lp.id.get, "learningpath", taxonomyBundle)

      val supportedLanguages = Language.getSupportedLanguages(lp.title, lp.description).toList
      val defaultTitle = lp.title.sortBy(title => ISO639.languagePriority.reverse.indexOf(title.language)).lastOption
      val license = api.learningpath.Copyright(
        asLearningPathApiLicense(lp.copyright.license),
        lp.copyright.contributors.map(c => api.learningpath.Author(c.`type`, c.name)))

      Success(
        SearchableLearningPath(
          id = lp.id.get,
          title = SearchableLanguageValues(lp.title.map(t => LanguageValue(t.language, t.title))),
          description = SearchableLanguageValues(lp.description.map(d => LanguageValue(d.language, d.description))),
          coverPhotoId = lp.coverPhotoId,
          duration = lp.duration,
          status = lp.status.toString,
          verificationStatus = lp.verificationStatus.toString,
          lastUpdated = lp.lastUpdated,
          defaultTitle = defaultTitle.map(_.title),
          tags = SearchableLanguageList(lp.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          learningsteps = lp.learningsteps.map(asSearchableLearningStep),
          license = license,
          isBasedOn = lp.isBasedOn,
          supportedLanguages = supportedLanguages,
          authors = lp.copyright.contributors.map(_.name).toList,
          contexts = taxonomyForLearningPath.getOrElse(List.empty)
        ))
    }

    def asSearchableDraft(draft: Draft, taxonomyBundle: Bundle): Try[SearchableDraft] = {
      val taxonomyForDraft = getTaxonomyContexts(draft.id.get, "article", taxonomyBundle)

      val defaultTitle = draft.title
        .sortBy(title => {
          ISO639.languagePriority.reverse.indexOf(title.language)
        })
        .lastOption

      val supportedLanguages = Language
        .getSupportedLanguages(
          draft.title,
          draft.visualElement,
          draft.introduction,
          draft.metaDescription,
          draft.content,
          draft.tags
        )
        .toList

      val authors = (
        draft.copyright.map(_.creators).toList ++
          draft.copyright.map(_.processors).toList ++
          draft.copyright.map(_.rightsholders).toList
      ).flatten.map(_.name)

      val statuses = List(draft.status.current) ++ draft.status.other
      val notes: List[String] = draft.notes.map(_.note)
      val users: List[String] = draft.updatedBy +: draft.notes.map(_.user)

      Success(
        SearchableDraft(
          id = draft.id.get,
          draftStatus = statuses.map(_.toString),
          title = SearchableLanguageValues(draft.title.map(title => LanguageValue(title.language, title.title))),
          content = SearchableLanguageValues(draft.content.map(article =>
            LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
          visualElement = SearchableLanguageValues(draft.visualElement.map(visual =>
            LanguageValue(visual.language, visual.resource))),
          introduction = SearchableLanguageValues(draft.introduction.map(intro =>
            LanguageValue(intro.language, intro.introduction))),
          metaDescription = SearchableLanguageValues(draft.metaDescription.map(meta =>
            LanguageValue(meta.language, meta.content))),
          tags = SearchableLanguageList(draft.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          lastUpdated = draft.updated,
          license = draft.copyright.flatMap(_.license),
          authors = authors,
          articleType = draft.articleType.toString,
          metaImage = draft.metaImage.toList,
          defaultTitle = defaultTitle.map(t => t.title),
          supportedLanguages = supportedLanguages,
          notes = notes,
          contexts = taxonomyForDraft.getOrElse(List.empty),
          users = users,
          previousVersionsNotes = draft.previousVersionsNotes.map(_.note)
        ))

    }

    def asLearningPathApiLicense(license: String): api.learningpath.License = {
      getLicense(license) match {
        case Some(l) => api.learningpath.License(l.license.toString, Option(l.description), l.url)
        case None    => api.learningpath.License(license, Some("Invalid license"), None)
      }
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      val nonHtmlDescriptions = learningStep.description.map(desc =>
        domain.learningpath.Description(Jsoup.parseBodyFragment(desc.description).text(), desc.language))
      SearchableLearningStep(
        learningStep.`type`.toString,
        SearchableLanguageValues(learningStep.title.map(t => LanguageValue(t.language, t.title))),
        SearchableLanguageValues(nonHtmlDescriptions.map(d => LanguageValue(d.language, d.description)))
      )
    }

    /**
      * Attempts to extract language that hit from highlights in elasticsearch response.
      *
      * @param result Elasticsearch hit.
      * @return Language if found.
      */
    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keySplits = keys.toList.flatMap(key => key.split('.'))
        val languagesInKeys = keySplits.filter(split => ISO639.languagePriority.contains(split))

        languagesInKeys
          .sortBy(lang => {
            ISO639.languagePriority.reverse.indexOf(lang)
          })
          .lastOption
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
      val introductions =
        searchableArticle.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableArticle.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableArticle.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages =
        searchableArticle.metaImage.map(im => api.article.ArticleMetaImage(im.imageId, im.altText, im.language))

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
      val visualElement = findByLanguageOrBestEffort(visualElements, language)
      val introduction = findByLanguageOrBestEffort(introductions, language)
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language)
      val metaImage = findByLanguageOrBestEffort(metaImages, language)

      val url = s"${SearchApiProperties.ExternalApiUrls("article-api")}/${searchableArticle.id}"

      ArticleSummary(
        searchableArticle.id,
        title,
        visualElement,
        introduction,
        metaDescription,
        metaImage,
        url,
        searchableArticle.license,
        searchableArticle.articleType,
        searchableArticle.supportedLanguages
      )
    }

    def hitAsDraftSummary(hitString: String, language: String): DraftSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

      val searchableDraft = read[SearchableDraft](hitString)

      val titles = searchableDraft.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val visualElements =
        searchableDraft.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val introductions =
        searchableDraft.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
      val visualElement = findByLanguageOrBestEffort(visualElements, language)
      val introduction = findByLanguageOrBestEffort(introductions, language)

      val url = s"${SearchApiProperties.ExternalApiUrls("draft-api")}/${searchableDraft.id}"

      DraftSummary(
        id = searchableDraft.id,
        title = title,
        visualElement = visualElement,
        introduction = introduction,
        url = url,
        license = searchableDraft.license.getOrElse(""),
        articleType = searchableDraft.articleType,
        supportedLanguages = searchableDraft.supportedLanguages,
        notes = searchableDraft.notes
      )
    }

    def hitAsLearningPathSummary(hitString: String, language: String): LearningPathSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableLearningPath = read[SearchableLearningPath](hitString)

      val titles = searchableLearningPath.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val descriptions =
        searchableLearningPath.description.languageValues.map(lv => api.learningpath.Description(lv.value, lv.language))
      val introductionStep = searchableLearningPath.learningsteps.find(_.stepType == StepType.INTRODUCTION.toString)
      val introductions = asApiLearningPathIntroduction(introductionStep)
      val tags =
        searchableLearningPath.tags.languageValues.map(lv => api.learningpath.LearningPathTags(lv.value, lv.language))

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
      val description = findByLanguageOrBestEffort(descriptions, language).getOrElse(
        api.learningpath.Description("", Language.UnknownLanguage))
      val introduction = findByLanguageOrBestEffort(introductions, language).getOrElse(
        api.learningpath.Introduction("", Language.UnknownLanguage))
      val tag = findByLanguageOrBestEffort(tags, language).getOrElse(
        api.learningpath.LearningPathTags(Seq.empty, Language.UnknownLanguage))

      val url = s"${SearchApiProperties.ExternalApiUrls("learningpath-api")}/${searchableLearningPath.id}"

      LearningPathSummary(
        searchableLearningPath.id,
        title,
        description,
        introduction,
        url,
        searchableLearningPath.coverPhotoId,
        searchableLearningPath.duration,
        searchableLearningPath.status,
        searchableLearningPath.lastUpdated,
        tag,
        searchableLearningPath.license,
        searchableLearningPath.supportedLanguages,
        searchableLearningPath.isBasedOn
      )

    }

    def asApiLearningPathIntroduction(
        learningStep: Option[SearchableLearningStep]): List[api.learningpath.Introduction] = {
      learningStep.map(_.description) match {
        case Some(desc) => desc.languageValues.map(lv => api.learningpath.Introduction(lv.value, lv.language)).toList
        case None       => List.empty
      }
    }

    def articleHitAsMultiSummary(hitString: String, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableArticle = read[SearchableArticle](hitString)

      val contexts = searchableArticle.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableArticle.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions =
        searchableArticle.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableArticle.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableArticle.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages = searchableArticle.metaImage.map(image => {
        val metaImageUrl = s"${SearchApiProperties.ExternalApiUrls("raw-image")}/${image.imageId}"
        api.MetaImage(metaImageUrl, image.altText, image.language)
      })

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", Language.UnknownLanguage))
      val metaImage = findByLanguageOrBestEffort(metaImages, language)

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

      val url = s"${SearchApiProperties.ExternalApiUrls("article-api")}/${searchableArticle.id}"

      MultiSearchSummary(
        id = searchableArticle.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        contexts = contexts,
        supportedLanguages = supportedLanguages
      )
    }

    def draftHitAsMultiSummary(hitString: String, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableDraft = read[SearchableDraft](hitString)

      val contexts = searchableDraft.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableDraft.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions =
        searchableDraft.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableDraft.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableDraft.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages = searchableDraft.metaImage.map(image => {
        val metaImageUrl = s"${SearchApiProperties.ExternalApiUrls("raw-image")}/${image.imageId}"
        api.MetaImage(metaImageUrl, image.altText, image.language)
      })

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", Language.UnknownLanguage))
      val metaImage = findByLanguageOrBestEffort(metaImages, language)

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

      val url = s"${SearchApiProperties.ExternalApiUrls("draft-api")}/${searchableDraft.id}"

      MultiSearchSummary(
        id = searchableDraft.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        contexts = contexts,
        supportedLanguages = supportedLanguages
      )
    }

    def learningpathHitAsMultiSummary(hitString: String, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableLearningPath = read[SearchableLearningPath](hitString)

      val contexts = searchableLearningPath.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableLearningPath.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val metaDescriptions =
        searchableLearningPath.description.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val tags =
        searchableLearningPath.tags.languageValues.map(lv => api.learningpath.LearningPathTags(lv.value, lv.language))

      val supportedLanguages = getSupportedLanguages(titles, metaDescriptions, tags)

      val title = findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", Language.UnknownLanguage))
      val url = s"${SearchApiProperties.ExternalApiUrls("learningpath-api")}/${searchableLearningPath.id}"
      val metaImage =
        searchableLearningPath.coverPhotoId.map(
          id =>
            api.MetaImage(
              url = s"${SearchApiProperties.ExternalApiUrls("raw-image")}/$id",
              alt = "",
              language = language
          ))

      MultiSearchSummary(
        id = searchableLearningPath.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        contexts = contexts,
        supportedLanguages = supportedLanguages
      )
    }

    def searchableContextToApiContext(context: SearchableTaxonomyContext, language: String): ApiTaxonomyContext = {
      val subjectName = findByLanguageOrBestEffort(context.subject.languageValues, language).map(_.value).getOrElse("")
      val breadcrumbs = findByLanguageOrBestEffort(context.breadcrumbs.languageValues, language)
        .map(_.value)
        .getOrElse(Seq.empty)
        .toList

      val resourceTypes = context.resourceTypes.map(rt => {
        val name = findByLanguageOrBestEffort(rt.name.languageValues, language)
          .getOrElse(LanguageValue(Language.UnknownLanguage, ""))
        TaxonomyResourceType(id = rt.id, name = name.value, language = name.language)
      })

      val filters = context.filters.map(filter => taxonomyFilterToApiFilter(filter, language))

      ApiTaxonomyContext(
        id = context.id,
        subject = subjectName,
        path = context.path,
        breadcrumbs = breadcrumbs,
        filters = filters,
        learningResourceType = context.contextType.toString,
        resourceTypes = resourceTypes,
        language = language
      )

    }

    def taxonomyFilterToApiFilter(filter: SearchableTaxonomyFilter, language: String): api.TaxonomyContextFilter = {
      val name = findByLanguageOrBestEffort(filter.name.languageValues, language).map(_.value).getOrElse("")
      val relevance = findByLanguageOrBestEffort(filter.relevance.languageValues, language).map(_.value).getOrElse("")

      api.TaxonomyContextFilter(
        name,
        relevance
      )
    }

    private def compareId(contentUri: String, id: Long, `type`: String): Boolean = {
      val split = contentUri.split(':')
      split match {
        case Array(_, cType: String, cId: String) => id.toString == cId && cType == `type`
        case _                                    => false
      }
    }

    private def getContextType(resourceId: String, contentUri: Option[String]): Try[LearningResourceType.Value] = {
      contentUri match {
        case Some(uri) if uri.contains("article") =>
          if (resourceId.contains(":topic:")) {
            Success(LearningResourceType.TopicArticle)
          } else {
            Success(LearningResourceType.Article)
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
    private def getFilters(resource: Resource,
                           subject: Resource,
                           bundle: Bundle,
                           objectFilterConnections: List[FilterConnection]): List[SearchableTaxonomyFilter] = {
      val subjectFilters = bundle.filters.filter(_.subjectId == subject.id)
      val filterConnections = objectFilterConnections
        .filter(_.objectId == resource.id)
        .filter(fc => subjectFilters.map(_.id).contains(fc.filterId))

      val connectedFilters = filterConnections.map(fc => (bundle.filters.find(_.id == fc.filterId), fc))

      connectedFilters.flatMap({
        case (Some(filter), filterConnection) =>
          val relevanceName = bundle.relevances
            .find(r => r.id == filterConnection.relevanceId)
            .map(_.name)
            .getOrElse("")

          Some(
            search.SearchableTaxonomyFilter(
              filterId = filter.id,
              name = SearchableLanguageValues(Seq(LanguageValue(Language.DefaultLanguage, filter.name))), // TODO: Get translations
              relevanceId = filterConnection.relevanceId,
              relevance = SearchableLanguageValues(Seq(LanguageValue(Language.DefaultLanguage, relevanceName))) // TODO: Get translations
            ))
        case _ => None
      })
    }

    /**
      * Returns every parent of resourceType.
      *
      * @param resourceType ResourceType to derive parents for.
      * @param allTypes All resourceTypes to derive parents from.
      * @return List of parents including resourceType.
      */
    def getResourceTypeParents(resourceType: ResourceType, allTypes: List[ResourceType]): List[ResourceType] = {
      def allTypesWithParents(allTypes: List[ResourceType],
                              parents: List[ResourceType]): List[(ResourceType, List[ResourceType])] = {
        allTypes.flatMap(resourceType => {
          val thisLevelWithParents = allTypes.map(resourceType => (resourceType, parents))

          val nextLevelWithParents = resourceType.subtypes match {
            case Some(subtypes) => allTypesWithParents(subtypes, parents :+ resourceType)
            case None           => List.empty
          }
          nextLevelWithParents ++ thisLevelWithParents
        })
      }
      allTypesWithParents(allTypes, List.empty).filter(x => x._1 == resourceType).flatMap(_._2).distinct
    }

    /**
      * Returns a flattened list of resourceType with its subtypes
      *
      * @param resourceType A resource with subtypes
      * @return Flattened list of resourceType with subtypes.
      */
    private def getTypeAndSubtypes(resourceType: ResourceType): List[ResourceType] = {
      def getTypeAndSubtypesWithParent(resourceType: ResourceType,
                                       parents: List[ResourceType] = List.empty): List[ResourceType] = {
        resourceType.subtypes match {
          case None => (parents :+ resourceType).distinct
          case Some(subtypes) =>
            subtypes.flatMap(x => getTypeAndSubtypesWithParent(x, parents :+ resourceType))
        }
      }
      getTypeAndSubtypesWithParent(resourceType, List.empty)
    }

    private def getResourceTaxonomyContexts(resource: Resource,
                                            bundle: Bundle): Try[List[SearchableTaxonomyContext]] = {
      val topicsConnections = bundle.topicResourceConnections.filter(_.resourceId == resource.id)
      val topics = bundle.topics.filter(topic => topicsConnections.map(_.topicid).contains(topic.id))
      val parentTopicsAndPaths = topics.flatMap(t => getParentTopicsAndPaths(t, bundle, List(t.id)))

      val resourceTypeConnections = bundle.resourceResourceTypeConnections.filter(_.resourceId == resource.id)
      val resourceTypesWithParents = getConnectedResourceTypesWithParents(resourceTypeConnections, bundle)

      getContextType(resource.id, resource.contentUri) match {
        case Success(contextType) =>
          val contexts = parentTopicsAndPaths.map({
            case (topic, topicPath) =>
              val subjectConnections = bundle.subjectTopicConnections.filter(_.topicid == topic.id)
              val subjects = bundle.subjects.filter(subject => subjectConnections.map(_.subjectid).contains(subject.id))

              subjects.map(subject => {
                val contextFilters = getFilters(resource, subject, bundle, bundle.resourceFilterConnections)
                val pathIds = (resource.id +: topicPath :+ subject.id).reverse

                getSearchableTaxonomyContext(resource.id,
                                             pathIds,
                                             subject,
                                             contextType,
                                             contextFilters,
                                             resourceTypesWithParents,
                                             bundle)
              })
          })
          Success(contexts.flatten)
        case Failure(ex) => Failure(ex)
      }
    }

    private def getSearchableTaxonomyContext(taxonomyId: String,
                                             pathIds: List[String],
                                             subject: Resource,
                                             contextType: LearningResourceType.Value,
                                             contextFilters: List[SearchableTaxonomyFilter],
                                             resourceTypes: List[ResourceType],
                                             bundle: Bundle): SearchableTaxonomyContext = {

      val path = "/" + pathIds.map(_.replace("urn:", "")).mkString("/")

      val searchableResourceTypes = resourceTypes.map(
        rt =>
          SearchableTaxonomyResourceType(
            id = rt.id,
            name = SearchableLanguageValues(Seq(LanguageValue(Language.DefaultLanguage, rt.name))) // TODO: Get translations
        ))

      val subjectLanguageValues = SearchableLanguageValues(Seq(LanguageValue(Language.DefaultLanguage, subject.name))) // TODO: Get translations
      val breadcrumbList = Seq(LanguageValue(
        Language.DefaultLanguage,
        getBreadcrumbFromIds(pathIds.dropRight(1), bundle))) // TODO: Get translations
      val breadcrumbs = SearchableLanguageList(breadcrumbList)

      val parentTopics = getAllParentTopicIds(taxonomyId, bundle)

      SearchableTaxonomyContext(
        id = taxonomyId,
        subjectId = subject.id,
        subject = subjectLanguageValues,
        path = path,
        contextType = contextType.toString,
        breadcrumbs = breadcrumbs,
        filters = contextFilters,
        resourceTypes = searchableResourceTypes,
        parentTopicIds = parentTopics
      )
    }

    private def getAllParentTopicIds(id: String, bundle: Bundle): List[String] = {
      val topicResourceConnections = bundle.topicResourceConnections.filter(_.resourceId == id)
      val topicSubtopicConnections = bundle.topicSubtopicConnections.filter(_.subtopicid == id)

      val directlyConnectedResourceTopics =
        bundle.topics.filter(t => topicResourceConnections.map(_.topicid).contains(t.id))
      val directlyConnectedTopicTopics =
        bundle.topics.filter(t => topicSubtopicConnections.map(_.topicid).contains(t.id))

      val allConnectedTopics = (directlyConnectedResourceTopics ++ directlyConnectedTopicTopics)
        .map(topic => getParentTopicsAndPaths(topic, bundle, List.empty))

      allConnectedTopics.flatMap(topic => topic.map(_._1)).map(_.id)
    }

    private def getConnectedResourceTypesWithParents(connections: List[ResourceTypeConnection], bundle: Bundle) = {
      val allResourceTypes = bundle.resourceTypes.flatMap(rt => getTypeAndSubtypes(rt))

      // Every explicitly specified resourceType
      val resourceTypes = allResourceTypes.filter(r => connections.map(_.resourceTypeId).contains(r.id))

      // Include parents of resourceTypes if they exist
      val subParents =
        resourceTypes.flatMap(rt => getResourceTypeParents(rt, bundle.resourceTypes)).filterNot(resourceTypes.contains)
      (resourceTypes ++ subParents).distinct
    }

    private def getTopicTaxonomyContexts(topic: Resource, bundle: Bundle): Try[List[SearchableTaxonomyContext]] = {
      val topicsConnections = bundle.topicResourceConnections.filter(_.resourceId == topic.id)
      val topics = bundle.topics.filter(topic => topicsConnections.map(_.topicid).contains(topic.id)) :+ topic
      val parentTopicsAndPaths = topics.flatMap(t => getParentTopicsAndPaths(t, bundle, List(t.id)))

      val resourceTypeConnections = bundle.topicResourceTypeConnections.filter(_.topicId == topic.id)
      val resourceTypesWithParents = getConnectedResourceTypesWithParents(resourceTypeConnections, bundle)

      getContextType(topic.id, topic.contentUri) match {
        case Success(contextType) =>
          val contexts = parentTopicsAndPaths.map({
            case (parentTopic, topicPath) =>
              val subjectConnections = bundle.subjectTopicConnections.filter(_.topicid == parentTopic.id)
              val subjects = bundle.subjects.filter(subject => subjectConnections.map(_.subjectid).contains(subject.id))

              subjects.map(subject => {
                val contextFilters = getFilters(topic, subject, bundle, bundle.topicFilterConnections)
                val pathIds = (topicPath :+ subject.id).reverse

                getSearchableTaxonomyContext(topic.id,
                                             pathIds,
                                             subject,
                                             contextType,
                                             contextFilters,
                                             resourceTypesWithParents,
                                             bundle)
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
    private[service] def getTaxonomyContexts(id: Long,
                                             taxonomyType: String,
                                             bundle: Bundle): Try[List[SearchableTaxonomyContext]] = {
      val (resources, topics) = getTaxonomyResourceAndTopicsForId(id, bundle, taxonomyType)
      val resourceContexts = resources.map(resource => getResourceTaxonomyContexts(resource, bundle))
      val topicContexts = topics.map(topic => getTopicTaxonomyContexts(topic, bundle))

      val all = resourceContexts ++ topicContexts
      val failed = all.collect {
            case Failure(e) => {
                  logger.error(s"Getting taxonomy context for $id failed with: ", e)
                  Failure(e)
            }
      }


      if (failed.nonEmpty) {
        failed.head
      } else {
        val successful = all.collect { case Success(c) => c }
        val distinctContexts = successful.flatten.distinct
        Success(distinctContexts)
      }
    }

    private def getTaxonomyResourceAndTopicsForId(id: Long, bundle: Bundle, taxonomyType: String) = {
      val resources = bundle.resources
        .filter(resource =>
          resource.contentUri match {
            case Some(contentUri) => compareId(contentUri, id, taxonomyType)
            case None             => false
        })
        .distinct

      val topics = bundle.topics
        .filter(topic =>
          topic.contentUri match {
            case Some(contentUri) => compareId(contentUri, id, taxonomyType)
            case None             => false
        })
        .distinct

      (resources, topics)
    }

    def toApiMultiSearchResult(searchResult: domain.SearchResult): MultiSearchResult =
      api.MultiSearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results,
      )

  }

}
