/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object SearchApiInfo {
  val apiInfo = ApiInfo(
  "Listing Api",
  "Documentation for the Search API of NDLA.no",
  "https://ndla.no",
  SearchApiProperties.ContactEmail,
  "GPL v3.0",
  "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class SearchSwagger extends Swagger("2.0", "0.8", SearchApiInfo.apiInfo) {
  addAuthorization(OAuth(List("listing:all"), List(ApplicationGrant(TokenEndpoint("/auth/tokens", "access_token")))))
}
