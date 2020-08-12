/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.{
  DefaultPageSize,
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  MaxPageSize
}
import no.ndla.searchapi.auth.{Role, User, UserInfo}
import no.ndla.searchapi.integration.SearchApiClient
import no.ndla.searchapi.model.api.{
  AccessDeniedException,
  Error,
  GroupSearchResult,
  GroupSummary,
  MultiSearchResult,
  SearchResults,
  ValidationError
}
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.{Language, SearchParams, Sort}
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.service.search.{
  MultiDraftSearchService,
  MultiSearchService,
  SearchConverterService,
  SearchService
}
import no.ndla.searchapi.service.{ApiSearchService, SearchClients}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.Ok
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra.util.NotNothing

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

trait SearchController {
  this: ApiSearchService
    with SearchClients
    with SearchApiClient
    with MultiSearchService
    with SearchConverterService
    with SearchService
    with MultiDraftSearchService
    with User =>
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

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query = Param[Option[String]]("query", "Return only results with content matching the specified query.")
    private val noteQuery =
      Param[Option[String]]("note-query", "Return only results with notes matching the specified note-query.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val license = Param[Option[String]]("license", "Return only results with provided license.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.values.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin
    )

    private val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize = Param[Option[Int]](
      "page-size",
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize.")
    private val resourceTypes = Param[Option[Seq[String]]](
      "resource-types",
      "Return only learning resources of specific type(s). To provide multiple types, separate by comma (,).")
    private val learningResourceIds = Param[Option[Seq[String]]](
      "ids",
      "Return only learning resources that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val apiTypes =
      Param[Option[Seq[String]]]("types", "A comma separated list of types to search in. f.ex articles,images")
    private val fallback = Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
    private val levels =
      Param[Option[Seq[String]]]("levels",
                                 "A comma separated list of levels the learning resources should be filtered by.")
    private val subjects =
      Param[Option[Seq[String]]]("subjects",
                                 "A comma separated list of subjects the learning resources should be filtered by.")
    private val topics =
      Param[Option[Seq[String]]](
        "topics",
        "A comma separated list of parent topics the learning resources should be filtered by.")
    private val contextTypes =
      Param[Option[Seq[String]]](
        "context-types",
        "A comma separated list of context-types the learning resources should be filtered by.")
    private val groupTypes =
      Param[Option[Seq[String]]](
        "resource-types",
        "A comma separated list of resource-types the learning resources should be grouped by.")
    private val languageFilter = Param[Option[Seq[String]]](
      "language-filter",
      "A comma separated list of ISO 639-1 language codes that the learning resource can be available in.")
    private val relevanceFilter = Param[Option[Seq[String]]](
      "relevance",
      """A comma separated list of relevances the learning resources should be filtered by.
        |If subjects are specified the learning resource must have specified relevances in relation to a specified subject.
        |If levels are specified the learning resource must have specified relevances in relation to a specified level.""".stripMargin
    )
    private val contextFilters = Param[Option[Seq[String]]](
      "context-filters",
      """A comma separated list of resource-types the learning resources should be filtered by.
        |Used in conjunction with the parameter resource-types to filter additional resource-types.
      """.stripMargin
    )

    private val grepCodes = Param[Option[Seq[String]]](
      "grep-codes",
      "A comma separated list of codes from GREP API the resources should be filtered by.")

    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A search context retrieved from the response header of a previous search.
        |If search-context is specified, all other query parameters, except '${this.language.paramName}' and '${this.fallback.paramName}' are ignored
        |For the rest of the parameters the original search of the search-context is used.
        |The search context may change between scrolls. Always use the most recent one (The context if unused dies after $ElasticSearchScrollKeepAlive).
        |Used to enable scrolling past $ElasticSearchIndexMaxResultWindow results.
      """.stripMargin
    )

    private val statusFilter = Param[Option[Seq[String]]](
      "draft-status",
      s"""List of statuses to filter by.
         |A draft only needs to have one of the available statuses to be included in result (OR).
         |Supported values are ${ArticleStatus.values.mkString(", ")}.""".stripMargin
    )

    private val userFilter = Param[Option[Seq[String]]](
      "users",
      s"""List of users to filter by.
         |The value to search for is the user-id from Auth0.
         |UpdatedBy on article and user in editorial-notes are searched.""".stripMargin
    )

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)

    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)

    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)

    get(
      "/group/",
      operation(
        apiOperation[Seq[GroupSearchResult]]("groupSearch")
          .summary("Search across multiple groups of learning resources")
          .description("Search across multiple groups of learning resources")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(groupTypes),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(language),
            asQueryParam(fallback),
            asQueryParam(subjects),
            asQueryParam(sort),
            asQueryParam(learningResourceIds),
            asQueryParam(levels),
            asQueryParam(contextTypes),
            asQueryParam(languageFilter),
            asQueryParam(relevanceFilter),
            asQueryParam(contextFilters)
          )
          .responseMessages(response500))
    ) {
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val resourceTypes = paramAsListOfString(this.groupTypes.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val query = paramOrNone(this.query.paramName)
      val subjects = paramAsListOfString(this.subjects.paramName)
      val sort = Sort
        .valueOf(paramOrDefault(this.sort.paramName, ""))
        .getOrElse(if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByIdAsc)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.languageFilter.paramName)
      val relevances = paramAsListOfString(this.relevanceFilter.paramName)
      val contexts = paramAsListOfString(this.contextFilters.paramName)
      val grepCodes = paramAsListOfString(this.grepCodes.paramName)

      val settings = SearchSettings(
        query = query,
        fallback = fallback,
        language = language,
        license = None,
        page = page,
        pageSize = pageSize,
        sort = sort,
        withIdIn = idList,
        taxonomyFilters = taxonomyFilters,
        subjects = subjects,
        resourceTypes = List.empty,
        learningResourceTypes = contextTypes.flatMap(LearningResourceType.valueOf),
        supportedLanguages = supportedLanguagesFilter,
        relevanceIds = relevances,
        contextIds = contexts,
        grepCodes = grepCodes
      )

      groupSearch(query, settings.copy(resourceTypes = resourceTypes))
    }

    private def searchInGroup(query: Option[String],
                              group: String,
                              settings: SearchSettings): Try[GroupSearchResult] = {

      multiSearchService
        .matchingQuery(settings)
        .map(
          searchResult =>
            GroupSearchResult(
              totalCount = searchResult.totalCount,
              resourceType = group,
              page = searchResult.page.getOrElse(1),
              pageSize = searchResult.pageSize,
              language = searchResult.language,
              results = searchResult.results.map(r => {
                val paths = r.contexts.map(_.path)
                GroupSummary(id = r.id, title = r.title, url = r.url, paths = paths)
              })
          ))
    }

    private def groupSearch(query: Option[String], settings: SearchSettings) = {
      if (settings.resourceTypes.nonEmpty) {
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(settings.resourceTypes.size))

        val searches = settings.resourceTypes.map(group =>
          Future { searchInGroup(query, group, settings.copy(resourceTypes = List(group))) })

        val futureSearches = Future.sequence(searches)
        val completedSearches = Await.result(futureSearches, Duration(10, SECONDS))

        val failedSearches = completedSearches.collect { case Failure(ex) => ex }
        if (failedSearches.nonEmpty) {
          errorHandler(failedSearches.head)
        } else {
          completedSearches.collect { case Success(r) => r }
        }
      } else {
        List.empty
      }
    }

    get(
      "/draft/",
      operation(
        apiOperation[Seq[SearchResults]]("searchAPIs")
          .summary("search across APIs")
          .description("search across APIs")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(language),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(apiTypes),
            asQueryParam(sort)
          )
          .responseMessages(response500))
    ) {
      val language = paramOrNone(this.language.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, "-relevance")).getOrElse(Sort.ByRelevanceDesc)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, 5)
      val apisToSearch: Set[SearchApiClient] =
        paramAsListOfString(this.apiTypes.paramName).flatMap(SearchClients.get).toSet match {
          case apiClients if apiClients.nonEmpty => apiClients
          case apiClients if apiClients.isEmpty  => SearchClients.values.toSet
        }

      val usedKeys =
        Set(this.language.paramName, this.pageNo.paramName, this.pageSize.paramName, this.apiTypes.paramName)
      val remainingParams = params(request).toMap.view.filterKeys(key => !usedKeys.contains(key)).toMap

      searchService.search(SearchParams(language, sort, page, pageSize, remainingParams), apisToSearch)
    }

    private def getSearchSettingsFromRequest = {
      val query = paramOrNone(this.query.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val resourceTypes = paramAsListOfString(this.resourceTypes.paramName)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val license = paramOrNone(this.license.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val subjects = paramAsListOfString(this.subjects.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.languageFilter.paramName)
      val relevances = paramAsListOfString(this.relevanceFilter.paramName)
      val contexts = paramAsListOfString(this.contextFilters.paramName)
      val grepCodes = paramAsListOfString(this.grepCodes.paramName)

      SearchSettings(
        query = query,
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
        supportedLanguages = supportedLanguagesFilter,
        relevanceIds = relevances,
        contextIds = contexts,
        grepCodes = grepCodes
      )
    }

    private def getDraftSearchSettingsFromRequest = {
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val resourceTypes = paramAsListOfString(this.resourceTypes.paramName)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val license = paramOrNone(this.license.paramName)
      val query = paramOrNone(this.query.paramName)
      val noteQuery = paramOrNone(this.noteQuery.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val subjects = paramAsListOfString(this.subjects.paramName)
      val topics = paramAsListOfString(this.topics.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.languageFilter.paramName)
      val relevances = paramAsListOfString(this.relevanceFilter.paramName)
      val statusFilter = paramAsListOfString(this.statusFilter.paramName)
      val userFilter = paramAsListOfString(this.userFilter.paramName)
      val grepCodes = paramAsListOfString(this.grepCodes.paramName)

      MultiDraftSearchSettings(
        query = query,
        noteQuery = noteQuery,
        fallback = fallback,
        language = language,
        license = license,
        page = page,
        pageSize = pageSize,
        sort = sort.getOrElse(if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByIdAsc),
        withIdIn = idList,
        taxonomyFilters = taxonomyFilters,
        subjects = subjects,
        topics = topics,
        resourceTypes = resourceTypes,
        learningResourceTypes = contextTypes.flatMap(LearningResourceType.valueOf),
        supportedLanguages = supportedLanguagesFilter,
        relevanceIds = relevances,
        statusFilter = statusFilter.flatMap(ArticleStatus.valueOf),
        userFilter = userFilter,
        grepCodes = grepCodes
      )
    }

    /**
      * Does a scroll with @scroller specified in the first parameter list
      * If no scrollId is specified execute the function @orFunction in the second parameter list.
      *
      * @param scroller SearchService to scroll with
      * @param orFunction Function to execute if no scrollId in parameters (Usually searching)
      * @tparam T SearchService
      * @return A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollWithOr[T <: SearchService](scroller: T)(orFunction: => Any): Any = {

      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      paramOrNone(this.scrollId.paramName) match {
        case Some(scroll) =>
          scroller.scroll(scroll, language, fallback) match {
            case Success(scrollResult) =>
              Ok(searchConverterService.toApiMultiSearchResult(scrollResult),
                 headers = scrollIdToHeader(scrollResult.scrollId))
            case Failure(ex) => errorHandler(ex)
          }
        case None => orFunction
      }
    }

    private def scrollIdToHeader(id: Option[String]) = id.map(i => this.scrollId.paramName -> i).toList.toMap

    get(
      "/",
      operation(
        apiOperation[MultiSearchResult]("searchLearningResources")
          .summary("Find learning resources")
          .description("Shows all learning resources. You can search too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(contextTypes),
            asQueryParam(language),
            asQueryParam(learningResourceIds),
            asQueryParam(resourceTypes),
            asQueryParam(levels),
            asQueryParam(license),
            asQueryParam(query),
            asQueryParam(sort),
            asQueryParam(fallback),
            asQueryParam(subjects),
            asQueryParam(languageFilter),
            asQueryParam(relevanceFilter),
            asQueryParam(contextFilters),
            asQueryParam(scrollId)
          )
          .responseMessages(response500))
    ) {
      scrollWithOr(multiSearchService) {
        multiSearchService.matchingQuery(getSearchSettingsFromRequest) match {
          case Success(searchResult) =>
            Ok(searchConverterService.toApiMultiSearchResult(searchResult), scrollIdToHeader(searchResult.scrollId))
          case Failure(ex) =>
            errorHandler(ex)
        }
      }
    }

    get(
      "/editorial/",
      operation(
        apiOperation[MultiSearchResult]("searchDraftLearningResources")
          .summary("Find draft learning resources")
          .description("Shows all draft learning resources. You can search too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(contextTypes),
            asQueryParam(language),
            asQueryParam(learningResourceIds),
            asQueryParam(resourceTypes),
            asQueryParam(levels),
            asQueryParam(license),
            asQueryParam(query),
            asQueryParam(noteQuery),
            asQueryParam(sort),
            asQueryParam(fallback),
            asQueryParam(subjects),
            asQueryParam(languageFilter),
            asQueryParam(relevanceFilter),
            asQueryParam(scrollId),
            asQueryParam(statusFilter),
            asQueryParam(userFilter),
            asQueryParam(grepCodes),
          )
          .responseMessages(response500))
    ) {
      if (!user.getUser.roles.contains(Role.DRAFTWRITE)) {
        errorHandler(AccessDeniedException("You do not have access to the requested resource."))
      } else {
        scrollWithOr(multiDraftSearchService) {
          multiDraftSearchService.matchingQuery(getDraftSearchSettingsFromRequest) match {
            case Success(searchResult) =>
              Ok(searchConverterService.toApiMultiSearchResult(searchResult), scrollIdToHeader(searchResult.scrollId))
            case Failure(ex) =>
              errorHandler(ex)
          }
        }
      }
    }
  }
}
