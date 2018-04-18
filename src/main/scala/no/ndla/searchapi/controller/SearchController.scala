/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import java.util.concurrent.{Executors, TimeUnit}

import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.SearchApiClient
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.api.learningpath.LearningPathSummary
import no.ndla.searchapi.model.api.{Error, GroupSearchResult, GroupSummary, MultiSearchResult, SearchResult, SearchResults, ValidationError}
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.{Language, SearchParams, Sort}
import no.ndla.searchapi.model.search.SearchSettings
import no.ndla.searchapi.service.search.{ArticleSearchService, LearningPathSearchService, MultiDraftSearchService, MultiSearchService}
import no.ndla.searchapi.service.{ApiSearchService, SearchClients}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport, SwaggerSupportSyntax}
import org.scalatra.util.NotNothing

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait SearchController {
  this: ApiSearchService
    with SearchClients
    with SearchApiClient
    with MultiSearchService
    with MultiDraftSearchService
    with ArticleSearchService
    with LearningPathSearchService =>
  val searchController: SearchController

  class SearchController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "API for searching across NDLA APIs"

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val correlationId = Param("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query = Param("query", "Return only results with content matching the specified query.")
    private val language = Param("language", "The ISO 639-1 language code describing language.")
    private val license = Param("license", "Return only results with provided license.")
    private val sort = Param("sort",
      """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin)
    private val pageNo = Param("page", "The page number of the search hits to display.")
    private val pageSize = Param("page-size", "The number of search hits to display for each page.")
    private val resourceTypes = Param("resource-types", "Return only learning resources of specific type(s). To provide multiple types, separate by comma (,).")
    private val learningResourceIds = Param("ids", "Return only learning resources that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val apiTypes = Param("types", "A comma separated list of types to search in. f.ex articles,images")
    private val fallback = Param("fallback", "Fallback to existing language if language is specified.")
    private val levels = Param("levels", "A comma separated list of levels the learning resources should be filtered by.")
    private val subjects = Param("subjects", "A comma separated list of subjects the learning resources should be filtered by.")
    private val contextTypes = Param("context-types", "A comma separated list of context-types the learning resources should be filtered by.")
    private val groupTypes = Param("resource-types", "A comma separated list of resource-types the learning resources should be grouped by.")
    private val languageFilter = Param("language-filter", "A comma separated list of ISO 639-1 language codes that the learning resource can be available in.")

    private val tag = Param("tag","Return only Learningpaths that are tagged with this exact tag.")

    private def asQueryParam[T: Manifest : NotNothing](param: Param) = queryParam[T](param.paramName).description(param.description)

    private def asHeaderParam[T: Manifest : NotNothing](param: Param) = headerParam[T](param.paramName).description(param.description)

    private def asPathParam[T: Manifest : NotNothing](param: Param) = pathParam[T](param.paramName).description(param.description)

    private val groupSearchDoc = (apiOperation[Seq[GroupSearchResult]]("groupSearch")
      summary "Search across multiple groups of learning resources"
      notes "Search across multiple groups of learning resources"
      parameters(
      asHeaderParam[Option[String]](correlationId),
      asQueryParam[Option[String]](query),
      asQueryParam[Option[String]](groupTypes),
      asQueryParam[Option[Int]](pageNo),
      asQueryParam[Option[Int]](pageSize),
      asQueryParam[Option[String]](language),
      asQueryParam[Option[Boolean]](fallback)
    )
      authorizations "oauth2"
      responseMessages response500
      )
    get("/group/", operation(groupSearchDoc)) {
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val resourceTypes = paramAsListOfString(this.groupTypes.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val query = paramOrNone(this.query.paramName)

      val settings = SearchSettings(
        fallback = fallback,
        language = language,
        license = None,
        page = page,
        pageSize = pageSize,
        sort = if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByIdAsc,
        withIdIn = List.empty,
        taxonomyFilters = List.empty,
        subjects = List.empty,
        resourceTypes = List.empty,
        learningResourceTypes = List.empty,
        supportedLanguages = List.empty
      )

      groupSearch(query, settings.copy(resourceTypes = resourceTypes))
    }

    private def searchInGroup(query: Option[String], group: String, settings: SearchSettings): Try[GroupSearchResult] = {
      val result = query match {
        case Some(q) => multiSearchService.matchingQuery(query = q, settings)
        case None => multiSearchService.all(settings)
      }

      result.map(searchResult => GroupSearchResult(
        totalCount = searchResult.totalCount,
        resourceType = group,
        page = searchResult.page,
        pageSize = searchResult.pageSize,
        language = searchResult.language,
        results = searchResult.results.map(r => {
          val paths = r.contexts.map(_.path)
          GroupSummary(id = r.id, title = r.title, url = r.url, paths = paths)
        })
      ))
    }

    private def groupSearch(query: Option[String], settings: SearchSettings) = {
      if(settings.resourceTypes.nonEmpty) {
        implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(settings.resourceTypes.size))

        val searches = settings.resourceTypes.map(group =>
          Future{searchInGroup(query, group, settings.copy(resourceTypes = List(group)))})

        val futureSearches = Future.sequence(searches)
        val completedSearches = Await.result(futureSearches, Duration(10, TimeUnit.SECONDS))

        val failedSearches = completedSearches.collect{case Failure(ex) => ex}
        if(failedSearches.nonEmpty) {
          errorHandler(failedSearches.head)
        } else {
          completedSearches.collect{case Success(r) => r}
        }
      } else {
        List.empty
      }
    }

    private val draftSearchDoc =
      (apiOperation[Seq[SearchResults]]("searchAPIs")
        summary "search across APIs"
        notes "search across APIs"
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[Int]](pageSize),
        asQueryParam[Option[String]](apiTypes)
      )
        authorizations "oauth2"
        responseMessages response500)
    get("/draft/", operation(draftSearchDoc)) {
      val language = paramOrDefault(this.language.paramName, "nb")
      val sort = Sort.ByRelevanceDesc
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, 5)
      val apisToSearch: Set[SearchApiClient] = paramAsListOfString(this.apiTypes.paramName).flatMap(SearchClients.get).toSet match {
        case apiClients if apiClients.nonEmpty => apiClients
        case apiClients if apiClients.isEmpty => SearchClients.values.toSet
      }

      val usedKeys = Set(this.language.paramName, this.pageNo.paramName, this.pageSize.paramName, this.apiTypes.paramName)
      val remainingParams = params(request).filterKeys(key => !usedKeys.contains(key))

      searchService.search(SearchParams(language, sort, page, pageSize, remainingParams), apisToSearch)
    }

    private def articleSearch(query: Option[String],
                              sort: Option[Sort.Value],
                              language: String,
                              license: Option[String],
                              page: Int,
                              pageSize: Int,
                              idList: List[Long],
                              articleTypesFilter: Seq[String],
                              fallback: Boolean) = {
      val result = query match {
        case Some(q) => articleSearchService.matchingQuery(
          query = q,
          withIdIn = idList,
          searchLanguage = language,
          license = license,
          page = page,
          pageSize = if (idList.isEmpty) pageSize else idList.size,
          sort = sort.getOrElse(Sort.ByRelevanceDesc),
          if (articleTypesFilter.isEmpty) LearningResourceType.all else articleTypesFilter,
          fallback = fallback
        )

        case None => articleSearchService.all(
          withIdIn = idList,
          language = language,
          license = license,
          page = page,
          pageSize = if (idList.isEmpty) pageSize else idList.size,
          sort = sort.getOrElse(Sort.ByIdAsc),
          if (articleTypesFilter.isEmpty) LearningResourceType.all else articleTypesFilter,
          fallback = fallback
        )
      }

      result match {
        case Success(searchResult) => searchResult
        case Failure(ex) => errorHandler(ex)
      }
    }

    private val articleSearchDoc =
      (apiOperation[SearchResult[ArticleSummary]]("getAllArticles")
        summary "Find articles"
        notes "Shows all articles. You can search it too."
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](sort),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[String]](license),
        asQueryParam[Option[Int]](pageSize),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[String]](learningResourceIds),
        asQueryParam[Option[String]](contextTypes),
        asQueryParam[Option[Boolean]](fallback)
      )
        authorizations "oauth2"
        responseMessages response500)
    get("/article/", operation(articleSearchDoc)) {
      val query = paramOrNone(this.query.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val license = paramOrNone(this.license.paramName)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val articleTypesFilter = paramAsListOfString(this.contextTypes.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      articleSearch(query, sort, language, license, page, pageSize, idList, articleTypesFilter, fallback)
    }

    private def learningPathSearch(query: Option[String],
                                   withIdIn: List[Long],
                                   taggedWith: Option[String],
                                   sort: Option[Sort.Value],
                                   language: String,
                                   page: Int,
                                   pageSize: Int,
                                   fallback: Boolean) = {

      val result = query match {
        case Some(q) =>
          learningPathSearchService.matchingQuery(
            query = q,
            withIdIn = withIdIn,
            taggedWith = taggedWith,
            language = language,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            page = page,
            pageSize = pageSize,
            fallback = fallback
          )
        case None =>
          learningPathSearchService.all(
            withIdIn = withIdIn,
            taggedWith = taggedWith,
            language = language,
            sort = sort.getOrElse(Sort.ByTitleAsc),
            page = page,
            pageSize = pageSize,
            fallback = fallback
          )
      }

      result match {
        case Success(searchResult) => searchResult
        case Failure(ex) => errorHandler(ex)
      }
    }

    private val learningPathSearchDoc = (apiOperation[SearchResult[LearningPathSummary]]("getLearningpaths")
      summary "Find learningpaths"
      notes "Shows all learningpaths. You can search too."
      parameters(
      asHeaderParam[Option[String]](correlationId),
      asQueryParam[Option[String]](query),
      asQueryParam[Option[String]](sort),
      asQueryParam[Option[String]](language),
      asQueryParam[Option[Int]](pageSize),
      asQueryParam[Option[Int]](pageNo),
      asQueryParam[Option[Long]](learningResourceIds),
      asQueryParam[Option[Boolean]](fallback),
      asQueryParam[Option[String]](tag)
    )
      authorizations "oauth2"
      responseMessages response500)
    get("/learningpath/", operation(learningPathSearchDoc)) {
      val query = paramOrNone(this.query.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val taggedWith = paramOrNone(this.tag.paramName)

      learningPathSearch(query, idList, taggedWith, sort, language, page, pageSize, fallback)
    }

    private val multiSearchDoc = (apiOperation[MultiSearchResult]("searchLearningResources")
      summary "Find learning resources"
      notes "Shows all learning resources. You can search too."
      parameters(
      asHeaderParam[Option[String]](correlationId),
      asQueryParam[Option[Int]](pageNo),
      asQueryParam[Option[Int]](pageSize),
      asQueryParam[Option[String]](contextTypes),
      asQueryParam[Option[String]](language),
      asQueryParam[Option[String]](learningResourceIds),
      asQueryParam[Option[String]](resourceTypes),
      asQueryParam[Option[String]](levels),
      asQueryParam[Option[String]](license),
      asQueryParam[Option[String]](query),
      asQueryParam[Option[String]](sort),
      asQueryParam[Option[Boolean]](fallback),
      asQueryParam[Option[String]](subjects),
      asQueryParam[Option[List[String]]](languageFilter)
    )
      authorizations "oauth2"
      responseMessages response500)
    get("/", operation(multiSearchDoc)) {
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val resourceTypes = paramAsListOfString(this.resourceTypes.paramName)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val license = paramOrNone(this.license.paramName)
      val query = paramOrNone(this.query.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val subjects = paramAsListOfString(this.subjects.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.languageFilter.paramName)

      val settings = SearchSettings(
        fallback = fallback,
        language = language,
        license = license,
        page = page,
        pageSize = pageSize,
        sort = sort.getOrElse(if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByIdAsc),
        withIdIn = idList,
        taxonomyFilters = taxonomyFilters,
        subjects = subjects,
        resourceTypes = resourceTypes,
        learningResourceTypes = contextTypes.flatMap(LearningResourceType.valueOf),
        supportedLanguages = supportedLanguagesFilter
      )
      multiSearch(query, settings)
    }

    private def multiSearch(query: Option[String], settings: SearchSettings) = {
      val result = query match {
        case Some(q) => multiSearchService.matchingQuery(query = q, settings)
        case None => multiSearchService.all(settings)
      }

      result match {
        case Success(searchResult) => searchResult
        case Failure(ex) => errorHandler(ex)
      }
    }

    private val multiSearchDraftDoc = (apiOperation[MultiSearchResult]("searchDraftLearningResources")
    summary "Find draft learning resources"
    notes "Shows all draft learning resources. You can search too."
    parameters(
      asHeaderParam[Option[String]](correlationId),
      asQueryParam[Option[Int]](pageNo),
      asQueryParam[Option[Int]](pageSize),
      asQueryParam[Option[String]](contextTypes),
      asQueryParam[Option[String]](language),
      asQueryParam[Option[String]](learningResourceIds),
      asQueryParam[Option[String]](resourceTypes),
      asQueryParam[Option[String]](levels),
      asQueryParam[Option[String]](license),
      asQueryParam[Option[String]](query),
      asQueryParam[Option[String]](sort),
      asQueryParam[Option[Boolean]](fallback),
      asQueryParam[Option[String]](subjects),
      asQueryParam[Option[List[String]]](languageFilter)
    )
      authorizations "oauth2"
      responseMessages response500)
    get("/editorial/", operation(multiSearchDraftDoc)) {
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val resourceTypes = paramAsListOfString(this.resourceTypes.paramName)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val license = paramOrNone(this.license.paramName)
      val query = paramOrNone(this.query.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val subjects = paramAsListOfString(this.subjects.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.languageFilter.paramName)

      val settings = SearchSettings(
        fallback = fallback,
        language = language,
        license = license,
        page = page,
        pageSize = pageSize,
        sort = sort.getOrElse(if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByIdAsc),
        withIdIn = idList,
        taxonomyFilters = taxonomyFilters,
        subjects = subjects,
        resourceTypes = resourceTypes,
        learningResourceTypes = contextTypes.flatMap(LearningResourceType.valueOf),
        supportedLanguages = supportedLanguagesFilter
      )
      multiDraftSearch(query, settings)
    }

    private def multiDraftSearch(query: Option[String], settings: SearchSettings) = {
      val result = query match {
        case Some(q) => multiDraftSearchService.matchingQuery(query = q, settings)
        case None => multiDraftSearchService.all(settings)
      }

      result match {
        case Success(searchResult) => searchResult
        case Failure(ex) => errorHandler(ex)
      }
    }

  }

}
