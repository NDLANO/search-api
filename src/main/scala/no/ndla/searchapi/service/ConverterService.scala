/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import no.ndla.searchapi.model.domain.{ApiSearchResults, ArticleApiSearchResult, ArticleApiSearchResults}
import no.ndla.searchapi.model.api

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {
    def searchResultToApiModel(searchResults: ApiSearchResults): api.SearchResults = {
      searchResults match {
        case a: ArticleApiSearchResults => articleSearchResultsToApi(a)
      }
    }

    private def articleSearchResultsToApi(articleRes: ArticleApiSearchResults): api.ArticleResults = {
      api.ArticleResults("articles", articleRes.results.map(articleSearchResultToApi))
    }

    private def articleSearchResultToApi(searchResult: ArticleApiSearchResult): api.ArticleResult ={
      api.ArticleResult(searchResult.id, searchResult.title, searchResult.introduction)
    }

  }
}
