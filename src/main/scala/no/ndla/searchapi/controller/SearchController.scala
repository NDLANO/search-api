/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.SearchApiClient
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.api.{Error, MultiSearchSummary, SearchResult, SearchResults, ValidationError}
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.{Language, SearchParams, Sort}
import no.ndla.searchapi.model.search.SearchSettings
import no.ndla.searchapi.service.search.{ArticleSearchService, MultiSearchService}
import no.ndla.searchapi.service.{ApiSearchService, SearchClients}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport, SwaggerSupportSyntax}
import org.scalatra.util.NotNothing

import scala.util.{Failure, Success}

trait SearchController {
  this: ApiSearchService
    with SearchClients
    with SearchApiClient
    with MultiSearchService
    with ArticleSearchService =>
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


    // TODO: Documentation for multi rather than article
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
    private val size = Param("size", "Limit the number of results to this many elements")
    private val learningResourceTypes = Param("learning-resource-types", "Return only learning resources of specific type(s). To provide multiple types, separate by comma (,).")
    private val learningResourceIds = Param("ids", "Return only learning resources that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val types = Param("types", "A comma separated list of types to search in. f.ex articles,images")
    private val fallback = Param("fallback", "Fallback to existing language if language is specified.")
    private val levels = Param("levels", "A comma separated list of levels the learning resources should be filtered by.")
    private val subjects = Param("subjects", "A comma separated list of subjects the learning resources should be filtered by.")
    private val resourceTypes = Param("resource-types", "A comma separated list of resource-types the learning resources should be filtered by.")
    private val contextTypes = Param("context-types", "A comma separated list of context-types the learning resources should be filtered by.")
    private val supportedLanguages = Param("language-filter", "A comma separated list of ISO 639-1 language codes that the learning resource can be available in.")

    private def asQueryParam[T: Manifest : NotNothing](param: Param) = queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest : NotNothing](param: Param) = headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest : NotNothing](param: Param) = pathParam[T](param.paramName).description(param.description)

    private val searchAPIs =
      (apiOperation[Seq[SearchResults]]("searchAPIs")
        summary "search across APIs"
        notes "search across APIs"
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[Int]](pageSize),
        asQueryParam[Option[String]](types)
      )
        authorizations "oauth2"
        responseMessages response500)

    get("/", operation(searchAPIs)) {
      val language = paramOrDefault(this.language.paramName, "nb")
      val sort = Sort.ByRelevanceDesc
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, 5)
      val apisToSearch: Set[SearchApiClient] = paramAsListOfString(this.types.paramName).flatMap(SearchClients.get).toSet match {
        case apiClients if apiClients.nonEmpty => apiClients
        case apiClients if apiClients.isEmpty => SearchClients.values.toSet
      }

      val usedKeys = Set(this.language.paramName, this.pageNo.paramName, this.pageSize.paramName, this.types.paramName)
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
        asQueryParam[Option[String]](learningResourceTypes),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](learningResourceIds),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[String]](license),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[Int]](pageSize),
        asQueryParam[Option[String]](sort)
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
      val articleTypesFilter = paramAsListOfString(this.learningResourceTypes.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      articleSearch(query, sort, language, license, page, pageSize, idList, articleTypesFilter, fallback)
    }


    private val multiSearchDoc = (apiOperation[SearchResult[MultiSearchSummary]]("searchLearningResources")
      summary "Find learning resources"
      notes "Shows all learning resources. You can search too."
      parameters(
        asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[Int]](pageSize),
        asQueryParam[Option[String]](contextTypes),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[String]](learningResourceIds),
        asQueryParam[Option[String]](learningResourceTypes),
        asQueryParam[Option[String]](levels),
        asQueryParam[Option[String]](license),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](resourceTypes),
        asQueryParam[Option[String]](sort),
        asQueryParam[Option[String]](subjects)
      )
      authorizations "oauth2"
      responseMessages response500)
    get("/content/", operation(multiSearchDoc)) {
      val query = paramOrNone(this.query.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val license = paramOrNone(this.license.paramName)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val subjects = paramAsListOfString(this.subjects.paramName)
      val resourceTypes = paramAsListOfString(this.resourceTypes.paramName)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.supportedLanguages.paramName)

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
        contextTypes = contextTypes.flatMap(LearningResourceType.valueOf),
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
  }

}
