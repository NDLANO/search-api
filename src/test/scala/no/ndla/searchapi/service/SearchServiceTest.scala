/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.network.ApplicationUrl
import no.ndla.network.model.HttpRequestException
import no.ndla.searchapi.model.domain.{SearchParams, Sort}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.model.api
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class SearchServiceTest extends UnitSuite with TestEnvironment {
  ApplicationUrl.applicationUrl.set("https://unit-test")
  override val searchService = new SearchService
  override val converterService = new ConverterService

  test("search should return a list of search results from other apis") {
    when(articleApiClient.search(any[SearchParams])).thenReturn(Future(Success(TestData.sampleArticleSearch)))
    when(learningpathApiClient.search(any[SearchParams])).thenReturn(Future(Success(TestData.sampleLearningpath)))

    val searchParams = SearchParams("nb", Sort.ByRelevanceDesc, 1, 10, Map.empty)
    val res = searchService.search(searchParams, Set(articleApiClient, learningpathApiClient))

    res.length should be (2)
    res.exists(ent => ent.isInstanceOf[api.ArticleResults]) should be (true)
    res.exists(ent => ent.isInstanceOf[api.LearningpathResults]) should be (true)
    res.exists(ent => ent.isInstanceOf[api.SearchError]) should be (false)
  }

  test("search should contain an error entry if a search failed") {
    when(articleApiClient.search(any[SearchParams])).thenReturn(Future(Failure(new HttpRequestException("Connection refused"))))
    when(learningpathApiClient.search(any[SearchParams])).thenReturn(Future(Success(TestData.sampleLearningpath)))
    when(imageApiClient.search(any[SearchParams])).thenReturn(Future(Success(TestData.sampleImageSearch)))
    when(audioApiClient.search(any[SearchParams])).thenReturn(Future(Success(TestData.sampleAudio)))

    val searchParams = SearchParams("nb", Sort.ByRelevanceDesc, 1, 10, Map.empty)
    val res = searchService.search(searchParams, SearchClients.values.toSet)

    res.length should be (4)
    res.exists(ent => ent.isInstanceOf[api.SearchError]) should be (true)
    res.exists(ent => ent.isInstanceOf[api.LearningpathResults]) should be (true)
    res.exists(ent => ent.isInstanceOf[api.ImageResults]) should be (true)
    res.exists(ent => ent.isInstanceOf[api.AudioResults]) should be (true)

    val error = res.find(ent => ent.isInstanceOf[api.SearchError]).get.asInstanceOf[api.SearchError]
    error.errorMsg should equal ("Connection refused")
  }

}
