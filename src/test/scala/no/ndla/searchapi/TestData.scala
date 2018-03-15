/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi

import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.domain.article._
import org.joda.time.DateTime

object TestData {
  private val publicDomainCopyright= Copyright("publicdomain", "", List(), List(), List(), None, None, None)
  private val byNcSaCopyright = Copyright("by-nc-sa", "Gotham City", List(Author("Writer", "DC Comics")), List(), List(), None, None, None)
  private val copyrighted = Copyright("copyrighted", "New York", List(Author("Writer", "Clark Kent")), List(), List(), None, None, None)
  private val today = new DateTime().toDate

  val sampleArticleTitle = ArticleApiTitle("tittell", "nb")
  val sampleArticleVisualElement = ArticleApiVisualElement("""<embed data-resource="image">""", "nb")
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
      ImageApiSearchResult("1", ImageTitle("title", "en"), ImageAltText("alt text", "en"), "http://images/1.jpg", "http://images/1", "by", Seq("en")),
      ImageApiSearchResult("1", ImageTitle("title", "en"), ImageAltText("alt text", "en"),  "http://images/1.jpg", "http://images/1", "by", Seq("en"))
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

  val (articleId, externalId) = (1, "751234")
  val sampleArticleWithPublicDomain = Article(
    Option(1),
    Option(1),
    Seq(ArticleTitle("test", "en")),
    Seq(ArticleContent("<section><div>test</div></section>", "en")),
    publicDomainCopyright,
    Seq(),
    Seq(),
    Seq(VisualElement("image", "en")),
    Seq(ArticleIntroduction("This is an introduction", "en")),
    Seq(ArticleMetaDescription("meta", "en")),
    None,
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "ndalId54321",
    LearningResourceType.Standard.toString)

  val sampleDomainArticle = Article(
    Option(articleId),
    Option(2),
    Seq(ArticleTitle("title", "nb")),
    Seq(ArticleContent("content", "nb")),
    Copyright("by", "", Seq(), Seq(), Seq(), None, None, None),
    Seq(ArticleTag(Seq("tag"), "nb")),
    Seq(),
    Seq(),
    Seq(),
    Seq(ArticleMetaDescription("meta description", "nb")),
    Some("11"),
    today,
    today,
    "ndalId54321",
    LearningResourceType.Standard.toString
  )

  val sampleDomainArticle2 = Article(
    None,
    None,
    Seq(ArticleTitle("test", "en")),
    Seq(ArticleContent("<article><div>test</div></article>", "en")),
    Copyright("publicdomain", "", Seq(), Seq(), Seq(), None, None, None),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    None,
    today,
    today,
    "ndalId54321",
    LearningResourceType.Standard.toString
  )
  val sampleArticleWithByNcSa: Article = sampleArticleWithPublicDomain.copy(copyright=byNcSaCopyright)
  val sampleArticleWithCopyrighted: Article = sampleArticleWithPublicDomain.copy(copyright=copyrighted )
}
