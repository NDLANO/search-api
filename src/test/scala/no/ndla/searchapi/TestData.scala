/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi

import no.ndla.searchapi.model.domain._

object TestData {
  val sampleArticleTitle = ArticleApiTitle("tittell", "nb")
  val sampleArticleVisualElement = ArticleApiVisualElment("""<embed data-resource="image">""", "nb")
  val sampleArticleIntro = ArticleApiIntro("intro", "nb")
  val sampleArticleSearch = ArticleApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      ArticleApiSearchResult(1, sampleArticleTitle, Option(sampleArticleVisualElement), Option(sampleArticleIntro), "http://articles/1", "by", "standard", Seq("nb", "en")),
      ArticleApiSearchResult(2, ArticleApiTitle("Another title", "nb"), Option(sampleArticleVisualElement), Option(sampleArticleIntro),  "http://articles/2", "by", "standard", Seq("nb", "en"))
    )
  )

  val sampleImageSearch = ImageApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      ImageApiSearchResult("1", ImageTitle("title", "en"), ImageAltText("alt text", "en"), "http://images/1.jpg", "http://images/1", "by"),
      ImageApiSearchResult("1", ImageTitle("title", "en"), ImageAltText("alt text", "en"),  "http://images/1.jpg", "http://images/1", "by")
    )
  )

  val sampleLearningpath = LearningpathApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      LearningpathApiSearchResult(1, LearningpathApiTitle("en title", "nb"), LearningpathApiDescription("en description", "nb"), LearningpathApiIntro("intro", "nb"), "http://learningpath/1", None, None, "PUBLISHED", "2016-07-06T09:08:08Z", LearningPathApiTags(Seq(), "nb"), Seq("nb"), None),
      LearningpathApiSearchResult(2, LearningpathApiTitle("en annen titlel", "nb"), LearningpathApiDescription("beskrivelse", "nb"), LearningpathApiIntro("itroduksjon", "nb"), "http://learningpath/2", None, None, "PUBLISHED", "2016-07-06T09:08:08Z", LearningPathApiTags(Seq(), "nb"), Seq("nb"), None),
    )
  )

  val sampleAudio = AudioApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      AudioApiSearchResult(1, AudioApiTitle("en title", "nb"), "http://audio/1", "by", Seq("nb")),
      AudioApiSearchResult(2, AudioApiTitle("ny tlttle", "nb"), "http://audio/2", "by", Seq("nb"))
    )
  )
}
