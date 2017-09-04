/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.searchapi.integration.SearchApiClient
import no.ndla.searchapi.model.api.{Error, SearchResults, ValidationError}
import no.ndla.searchapi.model.domain.{SearchParams, Sort}
import no.ndla.searchapi.service.{SearchClients, SearchService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait SearchController {
  this: SearchService with SearchClients with SearchApiClient =>
  val searchController: SearchController

  class SearchController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    registerModel[Error]
    registerModel[ValidationError]
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    protected val applicationDescription = "API for searching across NDLA APIs"

    val searchAPIs =
      (apiOperation[Seq[SearchResults]]("searchAPIs")
        summary "search across APIs"
        notes "search across APIs"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        queryParam[Option[String]]("query").description("Return only resources with content matching the specified query."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
      )
        authorizations "oauth2"
        responseMessages(response500))

    get("/", operation(searchAPIs)) {
      val query = paramOrNone("query")
      val language = paramOrNone("language")
      val sort = Sort.ByRelevanceDesc
      val page = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 5)
      val apisToSearch: Set[SearchApiClient] = paramAsListOfString("types").flatMap(SearchClients.get).toSet
      val allApis: Set[SearchApiClient] = SearchClients.values.toSet

      searchService.search(SearchParams(query, language, sort, page, pageSize), if (apisToSearch.isEmpty) allApis else apisToSearch)
    }

  }
}
