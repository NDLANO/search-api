/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.searchapi.model.domain.{SearchParams, Sort}
import no.ndla.searchapi.{SearchSwagger, TestEnvironment, UnitSuite}
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.search.settings.SearchSettings
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

import scala.util.Success

class SearchControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val swagger = new SearchSwagger

  lazy val controller = new SearchController
  addServlet(controller, "/test")

  test("That /draft/ returns 200 ok") {
    when(searchService.search(any[SearchParams], any[Set[SearchApiClient]])).thenReturn(Seq.empty)
    get("/test/draft/") {
      status should equal(200)
    }
  }

  test("That / returns 200 ok") {
    val multiResult = domain.SearchResult(0, Some(1), 10, "nb", Seq.empty)
    when(multiSearchService.all(any[SearchSettings])).thenReturn(Success(multiResult))
    get("/test/") {
      status should equal(200)
    }
  }

  test("That /group/ returns 200 ok") {
    val multiResult = domain.SearchResult(0, Some(1), 10, "nb", Seq.empty)
    when(multiSearchService.all(any[SearchSettings])).thenReturn(Success(multiResult))
    get("/test/group/?resource-types=test") {
      status should equal(200)
    }
  }

}
