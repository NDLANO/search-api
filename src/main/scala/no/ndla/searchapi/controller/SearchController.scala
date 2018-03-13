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
import no.ndla.searchapi.model.api.{Error, SearchResult, SearchResults, ValidationError}
import no.ndla.searchapi.model.domain.article.ArticleType
import no.ndla.searchapi.model.domain.{Language, SearchParams, Sort}
import no.ndla.searchapi.service.search.ArticleSearchService
import no.ndla.searchapi.service.{ApiSearchService, SearchClients}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport, SwaggerSupportSyntax}
import org.scalatra.util.NotNothing

import scala.util.{Failure, Success}

trait SearchController {
  this: ApiSearchService
    with SearchClients
    with SearchApiClient
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

    private val correlationId = Param("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query = Param("query", "Return only articles with content matching the specified query.")
    private val language = Param("language", "The ISO 639-1 language code describing language.")
    private val license = Param("license", "Return only results with provided license.")
    private val sort = Param("sort",
      """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin)
    private val pageNo = Param("page", "The page number of the search hits to display.")
    private val pageSize = Param("page-size", "The number of search hits to display for each page.")
    private val articleId = Param("article_id", "Id of the article that is to be fecthed")
    private val size = Param("size", "Limit the number of results to this many elements")
    private val articleTypes = Param("articleTypes", "Return only articles of specific type(s). To provide multiple types, separate by comma (,).")
    private val articleIds = Param("ids", "Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val deprecatedNodeId = Param("deprecated_node_id", "Id of deprecated NDLA node")
    private val fallback = Param("fallback", "Fallback to existing language if language is specified.")

    private def asQueryParam[T: Manifest : NotNothing](param: Param) = queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest : NotNothing](param: Param) = headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest : NotNothing](param: Param) = pathParam[T](param.paramName).description(param.description)

    val searchAPIs =
      (apiOperation[Seq[SearchResults]]("searchAPIs")
        summary "search across APIs"
        notes "search across APIs"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        queryParam[Option[String]]("query").description("Return only resources with content matching the specified query."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
        queryParam[Option[String]]("page").description("The page of each result set"),
        queryParam[Option[String]]("page-size").description("The page size of each result set"),
        queryParam[Option[String]]("types").description("A comma separated list of types to search in. f.ex articles,images"),
      )
        authorizations "oauth2"
        responseMessages (response500))

    get("/", operation(searchAPIs)) {
      val language = paramOrDefault("language", "nb")
      val sort = Sort.ByRelevanceDesc
      val page = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 5)
      val apisToSearch: Set[SearchApiClient] = paramAsListOfString("types").flatMap(SearchClients.get).toSet match {
        case apiClients if apiClients.nonEmpty => apiClients
        case apiClients if apiClients.isEmpty => SearchClients.values.toSet
      }

      val usedKeys = Set("language", "page", "page-size", "types")
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
          if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter,
          fallback = fallback
        )

        case None => articleSearchService.all(
          withIdIn = idList,
          language = language,
          license = license,
          page = page,
          pageSize = if (idList.isEmpty) pageSize else idList.size,
          sort = sort.getOrElse(Sort.ByIdAsc),
          if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter,
          fallback = fallback
        )
      }

      result match {
        case Success(searchResult) => searchResult
        case Failure(ex) => errorHandler(ex)
      }
    }

    val articleSearchDoc =
      (apiOperation[SearchResult[ArticleSummary]]("getAllArticles")
        summary "Find articles"
        notes "Shows all articles. You can search it too."
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](articleTypes),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](articleIds),
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
      val idList = paramAsListOfLong(this.articleIds.paramName)
      val articleTypesFilter = paramAsListOfString(this.articleTypes.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      articleSearch(query, sort, language, license, page, pageSize, idList, articleTypesFilter, fallback)
    }


    get("/multi/") { // TODO: Documentation
      val query = paramOrNone(this.query.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val license = paramOrNone(this.license.paramName)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val idList = paramAsListOfLong(this.articleIds.paramName)
      val articleTypesFilter = paramAsListOfString(this.articleTypes.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      


    }

  }

}
