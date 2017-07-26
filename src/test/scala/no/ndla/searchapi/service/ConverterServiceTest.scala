/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.model.api

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  test("searchResultToApiModel should return the api model of the corresponding input domain model") {
    converterService.searchResultToApiModel(TestData.sampleArticleSearch, "articles").isInstanceOf[api.ArticleResults]
    converterService.searchResultToApiModel(TestData.sampleLearningpath, "learningpaths").isInstanceOf[api.LearningpathResults]
    converterService.searchResultToApiModel(TestData.sampleImageSearch, "images").isInstanceOf[api.ImageResults]
    converterService.searchResultToApiModel(TestData.sampleAudio, "audios").isInstanceOf[api.AudioResults]
  }
}
