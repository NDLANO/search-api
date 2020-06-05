/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.searchapi.auth.{Role, UserInfo}
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.SearchParams
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.{SearchSwagger, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

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
    val multiResult = domain.SearchResult(0, Some(1), 10, "nb", Seq.empty, suggestions = Seq.empty)
    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    get("/test/") {
      status should equal(200)
    }
  }

  test("That /group/ returns 200 ok") {
    val multiResult = domain.SearchResult(0, Some(1), 10, "nb", Seq.empty, suggestions = Seq.empty)
    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    get("/test/group/?resource-types=test") {
      status should equal(200)
    }
  }

  test("That / returns scrollId if not scrolling, but scrollId is returned") {
    reset(multiSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nb", Seq.empty, Seq.empty, Some(validScrollId))

    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    get(s"/test/") {
      status should equal(200)
      response.headers("search-context").head should be(validScrollId)
    }

    verify(multiSearchService, times(1)).matchingQuery(any[SearchSettings])
  }

  test("That /editorial/ returns scrollId if not scrolling, but scrollId is returned") {
    reset(multiDraftSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nb", Seq.empty, Seq.empty, Some(validScrollId))

    when(multiDraftSearchService.matchingQuery(any[MultiDraftSearchSettings])).thenReturn(Success(multiResult))
    when(user.getUser).thenReturn(UserInfo("SomeId", Set(Role.DRAFTWRITE)))
    get(s"/test/editorial/") {
      status should equal(200)
      response.headers("search-context").head should be(validScrollId)
    }

    verify(multiDraftSearchService, times(1)).matchingQuery(any[MultiDraftSearchSettings])
  }

  test("That / scrolls if scrollId is specified") {
    reset(multiSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val newValidScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAtAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Some(newValidScrollId))

    when(multiSearchService.scroll(eqTo(validScrollId), eqTo("nn"), eqTo(true))).thenReturn(Success(multiResult))
    get(s"/test/?search-context=$validScrollId&language=nn&fallback=true") {
      status should equal(200)
      response.headers("search-context").head should be(newValidScrollId)
    }

    verify(multiSearchService, times(1)).scroll(eqTo(validScrollId), eqTo("nn"), eqTo(true))
  }

  test("That /editorial/ scrolls if scrollId is specified") {
    reset(multiDraftSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val newValidScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAtAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Some(newValidScrollId))

    when(multiDraftSearchService.scroll(eqTo(validScrollId), eqTo("nn"), eqTo(true))).thenReturn(Success(multiResult))
    when(user.getUser).thenReturn(UserInfo("SomeId", Set(Role.DRAFTWRITE)))
    get(s"/test/editorial/?search-context=$validScrollId&language=nn&fallback=true") {
      status should equal(200)
      response.headers("search-context").head should be(newValidScrollId)
    }

    verify(multiDraftSearchService, times(1)).scroll(eqTo(validScrollId), eqTo("nn"), eqTo(true))
  }

  test("That /editorial/ returns access denied if user does not have drafts:write role") {
    when(user.getUser).thenReturn(UserInfo("SomeId", Set()))
    get(s"/test/editorial/") {
      status should equal(403)
    }
  }

}
