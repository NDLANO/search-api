/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi

import no.ndla.searchapi.model.domain._

object TestData {
  val sampleArticleSearch = ArticleApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      ArticleApiSearchResult(1, "Tittel", "", "intro", "http://articles/1", "by", "standard", Seq("nb", "en")),
      ArticleApiSearchResult(2, "Another title", "", "intro", "http://articles/2", "by", "standard", Seq("nb", "en"))
    )
  )

  val sampleImageSearch = ImageApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    results = Seq(
      ImageApiSearchResult("1", "http://images/1.jpg", "http://images/1", "by"),
      ImageApiSearchResult("1", "http://images/1.jpg", "http://images/1", "by")
    )
  )

  val sampleLearningpath = LearningpathApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      LearningpathApiSearchResult(1, "en title", "en description", "intro", "http://learningpath/1", None, None, "PUBLISHED", "2016-07-06T09:08:08Z", Seq(), Seq("nb"), None),
      LearningpathApiSearchResult(2, "en annen titlel", "beskrivelse", "itroduksjon", "http://learningpath/2", None, None, "PUBLISHED", "2016-07-06T09:08:08Z", Seq(), Seq("nb"), None),
    )
  )

  val sampleAudio = AudioApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      AudioApiSearchResult(1, "en title", "http://audio/1", "by", Seq("nb")),
      AudioApiSearchResult(2, "ny tlttle", "http://audio/2", "by", Seq("nb"))
    )
  )
}
