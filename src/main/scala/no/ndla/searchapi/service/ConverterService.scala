/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.api

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {
    def searchResultToApiModel(searchResults: ApiSearchResults, apiName: String): api.SearchResults = {
      searchResults match {
        case a: ArticleApiSearchResults => articleSearchResultsToApi(a, apiName)
        case l: LearningpathApiSearchResults => learningpathSearchResultsToApi(l, apiName)
        case i: ImageApiSearchResults => imageSearchResultsToApi(i, apiName)
        case a: AudioApiSearchResults => audioSearchResultsToApi(a, apiName)
      }
    }

    private def articleSearchResultsToApi(articles: ArticleApiSearchResults, apiName: String): api.ArticleResults = {
      api.ArticleResults(apiName, articles.language, articles.results.map(articleSearchResultToApi))
    }

    private def articleSearchResultToApi(article: ArticleApiSearchResult): api.ArticleResult = {
      api.ArticleResult(article.id, article.title, article.introduction)
    }

    private def learningpathSearchResultsToApi(learningpaths: LearningpathApiSearchResults, apiName: String): api.LearningpathResults = {
      api.LearningpathResults(apiName, learningpaths.language, learningpaths.results.map(learningpathSearchResultToApi))
    }

    private def learningpathSearchResultToApi(learningpath: LearningpathApiSearchResult): api.LearningpathResult = {
      api.LearningpathResult(learningpath.id, learningpath.title, learningpath.introduction)
    }

    private def imageSearchResultsToApi(images: ImageApiSearchResults, apiName: String): api.ImageResults = {
      api.ImageResults(apiName, images.results.map(imageSearchResultToApi))
    }

    private def imageSearchResultToApi(image: ImageApiSearchResult): api.ImageResult = {
      api.ImageResult(image.id.toLong, image.previewUrl, image.metaUrl)
    }

    private def audioSearchResultsToApi(audios: AudioApiSearchResults, apiName: String): api.AudioResults = {
      api.AudioResults(apiName, audios.language, audios.results.map(audioSearchResultToApi))
    }

    private def audioSearchResultToApi(audio: AudioApiSearchResult): api.AudioResult = {
      api.AudioResult(audio.id, audio.title, audio.url)
    }

  }
}
