/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.api
import no.ndla.searchapi.SearchApiProperties.Domain
import no.ndla.network.ApplicationUrl
import com.netaporter.uri.dsl._

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {
    def searchResultToApiModel(searchResults: ApiSearchResults): api.SearchResults = {
      searchResults match {
        case a: ArticleApiSearchResults => articleSearchResultsToApi(a)
        case l: LearningpathApiSearchResults => learningpathSearchResultsToApi(l)
        case i: ImageApiSearchResults => imageSearchResultsToApi(i)
        case a: AudioApiSearchResults => audioSearchResultsToApi(a)
      }
    }

    private def articleSearchResultsToApi(articles: ArticleApiSearchResults): api.ArticleResults = {
      api.ArticleResults("articles", articles.language, articles.results.map(articleSearchResultToApi))
    }

    private def articleSearchResultToApi(article: ArticleApiSearchResult): api.ArticleResult = {
      api.ArticleResult(article.id, article.title.title, article.introduction.map(_.introduction))
    }

    private def learningpathSearchResultsToApi(learningpaths: LearningpathApiSearchResults): api.LearningpathResults = {
      api.LearningpathResults("learningpaths", learningpaths.language, learningpaths.results.map(learningpathSearchResultToApi))
    }

    private def learningpathSearchResultToApi(learningpath: LearningpathApiSearchResult): api.LearningpathResult = {
      api.LearningpathResult(learningpath.id, learningpath.title.title, learningpath.introduction.introduction)
    }

    private def imageSearchResultsToApi(images: ImageApiSearchResults): api.ImageResults = {
      api.ImageResults("images", images.results.map(imageSearchResultToApi))
    }

    private def imageSearchResultToApi(image: ImageApiSearchResult): api.ImageResult = {
      val scheme = ApplicationUrl.get.scheme.getOrElse("https://")
      val host = ApplicationUrl.get.host.getOrElse(Domain)

      val previewUrl = image.previewUrl.withHost(host).withScheme(scheme)
      val metaUrl = image.metaUrl.withHost(host).withScheme(scheme)

      api.ImageResult(image.id.toLong, previewUrl, metaUrl)
    }

    private def audioSearchResultsToApi(audios: AudioApiSearchResults): api.AudioResults = {
      api.AudioResults("audios", audios.language, audios.results.map(audioSearchResultToApi))
    }

    private def audioSearchResultToApi(audio: AudioApiSearchResult): api.AudioResult = {
      val scheme = ApplicationUrl.get.scheme.getOrElse("https://")
      val host = ApplicationUrl.get.host.getOrElse(Domain)

      val url = audio.url.withHost(host).withScheme(scheme)
      api.AudioResult(audio.id, audio.title, url)
    }

  }
}
