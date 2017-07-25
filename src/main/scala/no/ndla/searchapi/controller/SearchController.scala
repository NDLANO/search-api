/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.searchapi.model.api.ValidationError
import no.ndla.searchapi.model.domain.Sort
import no.ndla.searchapi.SearchApiProperties.DefaultLanguage
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.Ok
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait SearchController {
  val searchController: SearchController

  class SearchController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))

    registerModel[Error]
    registerModel[ValidationError]
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    protected val applicationDescription = "API for searching across NDLA APIs"

    val searchAPIs =
      (apiOperation[String]("searchAPIs")
        summary "search across APIs"
        notes "search across APIs"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
        queryParam[Option[String]]("query").description("Return only resources with content matching the specified query."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
      )
        authorizations "oauth2"
        responseMessages(response500))

    get("/", operation(searchAPIs)) {
      val query = paramAsListOfString("query")
      val language = paramOrDefault("language", DefaultLanguage)
      val sort = Sort.ByRelevanceDesc

      Ok("Hello search-api")
    }

  }
}
