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
import no.ndla.searchapi.model.api.MultiSearchResult
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.api.learningpath.LearningPathSummary
import no.ndla.searchapi.model.search.SearchSettings
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.Success


class SearchControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val swagger = new SearchSwagger

  lazy val controller = new SearchController
  addServlet(controller, "/test")

  test("That /draft/ returns 200 ok") {
    when(searchService.search(any[SearchParams], any[Set[SearchApiClient]])).thenReturn(Seq.empty)
    get("/test/draft/") {
      status should equal (200)
    }
  }

  test("That /article/ returns 200 ok") {
    val articleResult = api.SearchResult[ArticleSummary](0,1,10,"nb",Seq.empty)
    when(articleSearchService.all(any[List[Long]], any[String], any[Option[String]], any[Int], any[Int], any[Sort.Value], any[Seq[String]], any[Boolean])).thenReturn(Success(articleResult))

    get("/test/article/") {
      status should equal (200)
    }
  }

  test("That /learningpath/ returns 200 ok") {
    val lpResult = api.SearchResult[LearningPathSummary](0,1,10,"nb", Seq.empty)
    when(learningPathSearchService.all(any[List[Long]], any[Option[String]], any[String], any[Sort.Value], any[Int], any[Int], any[Boolean])).thenReturn(Success(lpResult))
    get("/test/learningpath/") {
      status should equal (200)
    }
  }

  test("That / returns 200 ok") {
    val multiResult = api.MultiSearchResult(0, 0, 0, 0, 1, 10, "nb", Seq.empty)
    when(multiSearchService.all(any[SearchSettings])).thenReturn(Success(multiResult))
    get("/test/") {
      status should equal (200)
    }
  }

  test("That /group/ returns 200 ok") {
    val multiResult = api.MultiSearchResult(0, 0, 0, 0, 1, 10, "nb", Seq.empty)
    when(multiSearchService.all(any[SearchSettings])).thenReturn(Success(multiResult))
    get("/test/group/?resource-types=test") {
      status should equal (200)
    }
  }


}
