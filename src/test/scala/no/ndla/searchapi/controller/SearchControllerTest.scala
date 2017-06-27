/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.searchapi.{SearchSwagger, TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite

class SearchControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val swagger = new SearchSwagger

  lazy val controller = new SearchController
  addServlet(controller, "/")

  test("That /search returns 200 ok") {
    get("/") {
      status should equal (200)
    }
  }

}
