/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.searchapi.model.domain.{Tag, Title}
import no.ndla.searchapi.model.domain.article.{Article, ArticleContent}
import no.ndla.searchapi.model.grep.{GrepElement, GrepTitle}
import no.ndla.searchapi.model.search.{
  SearchableArticle,
  SearchableGrepContext,
  SearchableLanguageList,
  SearchableLanguageValues
}
import no.ndla.searchapi.model.taxonomy._
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock

import scala.util.Success

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {

  override val searchConverterService = new SearchConverterService
  val sampleArticle = TestData.sampleArticleWithPublicDomain.copy()

  val titles = List(
    Title("Bokmål tittel", "nb"),
    Title("Nynorsk tittel", "nn"),
    Title("English title", "en"),
    Title("Titre francais", "fr"),
    Title("Deutsch titel", "de"),
    Title("Titulo espanol", "es"),
    Title("Nekonata titolo", "unknown")
  )

  val articles = Seq(
    ArticleContent("Bokmål artikkel", "nb"),
    ArticleContent("Nynorsk artikkel", "nn"),
    ArticleContent("English article", "en"),
    ArticleContent("Francais article", "fr"),
    ArticleContent("Deutsch Artikel", "de"),
    ArticleContent("Articulo espanol", "es"),
    ArticleContent("Nekonata artikolo", "unknown")
  )

  val articleTags = Seq(
    Tag(Seq("fugl", "fisk"), "nb"),
    Tag(Seq("fugl", "fisk"), "nn"),
    Tag(Seq("bird", "fish"), "en"),
    Tag(Seq("got", "tired"), "fr"),
    Tag(Seq("of", "translating"), "de"),
    Tag(Seq("all", "of"), "es"),
    Tag(Seq("the", "words"), "unknown")
  )

  val visibleMetadata = Some(Metadata(Seq.empty, visible = true))
  val invisibleMetadata = Some(Metadata(Seq.empty, visible = false))

  val resources = List(
    Resource("urn:resource:1", "Resource1", Some("urn:article:1"), "/subject:1/topic:10/resource:1", visibleMetadata))

  val topics = List(Topic("urn:topic:10", "Topic1", Some("urn:article:10"), "/subject:1/topic:10", visibleMetadata))

  val topicResourceConnections = List(
    TopicResourceConnection("urn:topic:10", "urn:resource:1", "urn:topic-resource:abc123", true, 1))
  val subject1 = TaxSubject("urn:subject:1", "Subject1", None, "/subject:1", visibleMetadata)
  val subjects = List(subject1)

  val subjectTopicConnections = List(
    SubjectTopicConnection("urn:subject:1", "urn:topic:10", "urn:subject-topic:8180abc", true, 1))

  val emptyBundle = TaxonomyBundle(
    filters = List.empty,
    relevances = List.empty,
    resourceFilterConnections = List.empty,
    resourceResourceTypeConnections = List.empty,
    resourceTypes = List.empty,
    resources = resources,
    subjectTopicConnections = subjectTopicConnections,
    subjects = subjects,
    topicFilterConnections = List.empty,
    topicResourceConnections = topicResourceConnections,
    topicSubtopicConnections = List.empty,
    topicResourceTypeConnections = List.empty,
    topics = topics
  )

  override def beforeAll(): Unit = {
    when(converterService.withAgreementCopyright(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      invocation.getArgument[Article](0))

    when(taxonomyApiClient.getTaxonomyBundle).thenReturn(Success(emptyBundle))
  }

  test("That asSearchableArticle converts titles with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = titles)
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, TestData.emptyGrepBundle)
    verifyTitles(searchableArticle)
  }

  test("That asSearchable converts articles with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = articles)
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, TestData.emptyGrepBundle)
    verifyArticles(searchableArticle)
  }

  test("That asSearchable converts tags with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags = articleTags)
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, TestData.emptyGrepBundle)
    verifyTags(searchableArticle)
  }

  test("That asSearchable converts all fields with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = titles, content = articles, tags = articleTags)
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, TestData.emptyGrepBundle)

    verifyTitles(searchableArticle)
    verifyArticles(searchableArticle)
    verifyTags(searchableArticle)
  }

  test("That asSearchableArticle converts titles with license from agreement") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = titles)
    when(converterService.withAgreementCopyright(any[Article]))
      .thenReturn(article.copy(copyright = article.copyright.copy(license = "gnu")))
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, TestData.emptyGrepBundle)
    searchableArticle.license should equal("gnu")
  }

  test("That resource types are derived correctly") {
    val Success(searchable2) =
      searchConverterService.asSearchableArticle(TestData.article2,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)
    val Success(searchable7) =
      searchConverterService.asSearchableArticle(TestData.article7,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)

    searchable2.contexts.head.resourceTypes.map(_.id).sorted should be(
      Seq("urn:resourcetype:subjectMaterial", "urn:resourcetype:academicArticle").sorted)
    searchable4.contexts.head.resourceTypes.map(_.id).sorted should be(Seq("urn:resourcetype:subjectMaterial").sorted)
    searchable7.contexts.head.resourceTypes.map(_.id).sorted should be(
      Seq(
        "urn:resourcetype:nested",
        "urn:resourcetype:peerEvaluation",
        "urn:resourcetype:reviewResource",
        "urn:resourcetype:guidance",
        "urn:resourcetype:subjectMaterial"
      ).sorted)
  }

  test("That breadcrumbs are derived correctly") {
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)
    val Success(searchable6) =
      searchConverterService.asSearchableArticle(TestData.article6,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)

    searchable1.contexts.size should be(2)
    searchable1.contexts.head.breadcrumbs.languageValues.map(_.value) should be(
      Seq(
        Seq(
          "Matte",
          "Baldur har mareritt"
        )))

    searchable1.contexts(1).breadcrumbs.languageValues.map(_.value) should be(
      Seq(
        Seq(
          "Historie",
          "Katter"
        )))

    searchable4.contexts.size should be(1)
    searchable4.contexts.head.breadcrumbs.languageValues.map(_.value) should be(
      Seq(
        Seq(
          "Matte",
          "Baldur har mareritt",
          "En Baldur har mareritt om Ragnarok"
        )))

    searchable6.contexts.size should be(1)
    searchable6.contexts.head.breadcrumbs.languageValues.map(_.value) should be(
      Seq(
        Seq(
          "Historie",
          "Katter"
        )))
  }

  test("That subjects are derived correctly from taxonomy") {
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)
    val Success(searchable5) =
      searchConverterService.asSearchableArticle(TestData.article5,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)

    searchable1.contexts.size should be(2)
    searchable1.contexts.head.subject.languageValues.map(_.value) should be(Seq("Matte"))
    searchable1.contexts(1).subject.languageValues.map(_.value) should be(Seq("Historie"))

    searchable4.contexts.size should be(1)
    searchable4.contexts.head.subject.languageValues.map(_.value) should be(Seq("Matte"))

    searchable5.contexts.size should be(2)
    searchable5.contexts.head.subject.languageValues.map(_.value) should be(Seq("Matte"))
    searchable5.contexts(1).subject.languageValues.map(_.value) should be(Seq("Historie"))
  }

  test("That invisible contexts are not indexed") {
    val taxonomyBundleInvisibleMetadata = TestData.taxonomyTestBundle.copy(resources = resources.map(resource =>
      resource.copy(metadata = invisibleMetadata)))
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 taxonomyBundleInvisibleMetadata,
                                                 TestData.emptyGrepBundle)
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 taxonomyBundleInvisibleMetadata,
                                                 TestData.emptyGrepBundle)
    val Success(searchable5) =
      searchConverterService.asSearchableArticle(TestData.article5,
                                                 taxonomyBundleInvisibleMetadata,
                                                 TestData.emptyGrepBundle)

    searchable1.contexts.size should be(0)
    searchable4.contexts.size should be(0)
    searchable5.contexts.size should be(0)
  }

  test("That invisible subjects are not indexed") {
    val taxonomyBundleInvisibleMetadata =
      TestData.taxonomyTestBundle.copy(subjects = subjects.map(subject => subject.copy(metadata = invisibleMetadata)))
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 taxonomyBundleInvisibleMetadata,
                                                 TestData.emptyGrepBundle)
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 taxonomyBundleInvisibleMetadata,
                                                 TestData.emptyGrepBundle)
    val Success(searchable5) =
      searchConverterService.asSearchableArticle(TestData.article5,
                                                 taxonomyBundleInvisibleMetadata,
                                                 TestData.emptyGrepBundle)

    searchable1.contexts.size should be(0)
    searchable4.contexts.size should be(0)
    searchable5.contexts.size should be(0)
  }

  test("That taxonomy filters are derived correctly") {
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)
    val Success(searchable5) =
      searchConverterService.asSearchableArticle(TestData.article5,
                                                 TestData.taxonomyTestBundle,
                                                 TestData.emptyGrepBundle)

    searchable1.contexts.size should be(2)
    searchable1.contexts.head.filters.map(_.name.languageValues.map(_.value)) should be(
      Seq(Seq("VG1"), Seq("VG2"), Seq("VG3"), Seq("Tysk 2")))
    searchable1.contexts(1).filters.map(_.name.languageValues.map(_.value)) should be(Seq.empty)

    searchable4.contexts.size should be(1)
    searchable4.contexts.head.filters.map(_.name.languageValues.map(_.value)) should be(Seq(Seq("VG3")))

    searchable5.contexts.size should be(2)
    searchable5.contexts.head.filters.map(_.name.languageValues.map(_.value)) should be(Seq(Seq("VG2")))
    searchable5.contexts(1).filters.map(_.name.languageValues.map(_.value)) should be(Seq(Seq("VG1"), Seq("VG2")))
  }

  test("That asSearchableArticle converts grepContexts correctly based on article grepCodes if grepBundle is empty") {
    val article = TestData.emptyDomainArticle.copy(id = Some(99), grepCodes = Seq("KE12", "KM123", "TT2"))
    val grepContexts = List(SearchableGrepContext("KE12", None),
                            SearchableGrepContext("KM123", None),
                            SearchableGrepContext("TT2", None))
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, TestData.emptyGrepBundle)
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableArticle converts grepContexts correctly based on grepBundle if article has grepCodes") {
    val article = TestData.emptyDomainArticle.copy(id = Some(99), grepCodes = Seq("KE12", "KM123", "TT2"))
    val grepBundle = TestData.emptyGrepBundle.copy(
      kjerneelementer = List(GrepElement("KE12", Seq(GrepTitle("default", "tittel12"))),
                             GrepElement("KE34", Seq(GrepTitle("default", "tittel34")))),
      kompetansemaal = List(GrepElement("KM123", Seq(GrepTitle("default", "tittel123")))),
      tverrfagligeTemaer = List(GrepElement("TT2", Seq(GrepTitle("default", "tittel2"))))
    )
    val grepContexts = List(SearchableGrepContext("KE12", Some("tittel12")),
                            SearchableGrepContext("KM123", Some("tittel123")),
                            SearchableGrepContext("TT2", Some("tittel2")))
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, grepBundle)
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableArticle converts grepContexts correctly based on grepBundle if article has no grepCodes") {
    val article = TestData.emptyDomainArticle.copy(id = Some(99), grepCodes = Seq.empty)
    val grepBundle = TestData.emptyGrepBundle.copy(
      kjerneelementer = List(GrepElement("KE12", Seq(GrepTitle("default", "tittel12"))),
                             GrepElement("KE34", Seq(GrepTitle("default", "tittel34")))),
      kompetansemaal = List(GrepElement("KM123", Seq(GrepTitle("default", "tittel123")))),
      tverrfagligeTemaer = List(GrepElement("TT2", Seq(GrepTitle("default", "tittel2"))))
    )
    val grepContexts = List.empty

    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, grepBundle)
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableDraft converts grepContexts correctly based on draft grepCodes if grepBundle is empty") {
    val draft = TestData.emptyDomainDraft.copy(id = Some(99), grepCodes = Seq("KE12", "KM123", "TT2"))
    val grepContexts = List(SearchableGrepContext("KE12", None),
                            SearchableGrepContext("KM123", None),
                            SearchableGrepContext("TT2", None))
    val Success(searchableArticle) =
      searchConverterService.asSearchableDraft(draft, emptyBundle, TestData.emptyGrepBundle)
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableDraft converts grepContexts correctly based on grepBundle if draft has grepCodes") {
    val draft = TestData.emptyDomainDraft.copy(id = Some(99), grepCodes = Seq("KE12", "KM123", "TT2"))
    val grepBundle = TestData.emptyGrepBundle.copy(
      kjerneelementer = List(GrepElement("KE12", Seq(GrepTitle("default", "tittel12"))),
                             GrepElement("KE34", Seq(GrepTitle("default", "tittel34")))),
      kompetansemaal = List(GrepElement("KM123", Seq(GrepTitle("default", "tittel123")))),
      tverrfagligeTemaer = List(GrepElement("TT2", Seq(GrepTitle("default", "tittel2"))))
    )
    val grepContexts = List(SearchableGrepContext("KE12", Some("tittel12")),
                            SearchableGrepContext("KM123", Some("tittel123")),
                            SearchableGrepContext("TT2", Some("tittel2")))
    val Success(searchableArticle) =
      searchConverterService.asSearchableDraft(draft, emptyBundle, grepBundle)
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableDraft converts grepContexts correctly based on grepBundle if draft has no grepCodes") {
    val draft = TestData.emptyDomainDraft.copy(id = Some(99), grepCodes = Seq.empty)
    val grepBundle = TestData.emptyGrepBundle.copy(
      kjerneelementer = List(GrepElement("KE12", Seq(GrepTitle("default", "tittel12"))),
                             GrepElement("KE34", Seq(GrepTitle("default", "tittel34")))),
      kompetansemaal = List(GrepElement("KM123", Seq(GrepTitle("default", "tittel123")))),
      tverrfagligeTemaer = List(GrepElement("TT2", Seq(GrepTitle("default", "tittel2"))))
    )
    val grepContexts = List.empty

    val Success(searchableArticle) =
      searchConverterService.asSearchableDraft(draft, emptyBundle, grepBundle)
    searchableArticle.grepContexts should equal(grepContexts)
  }

  private def verifyTitles(searchableArticle: SearchableArticle): Unit = {
    searchableArticle.title.languageValues.size should equal(titles.size)
    languageValueWithLang(searchableArticle.title, "nb") should equal(titleForLang(titles, "nb"))
    languageValueWithLang(searchableArticle.title, "nn") should equal(titleForLang(titles, "nn"))
    languageValueWithLang(searchableArticle.title, "en") should equal(titleForLang(titles, "en"))
    languageValueWithLang(searchableArticle.title, "fr") should equal(titleForLang(titles, "fr"))
    languageValueWithLang(searchableArticle.title, "de") should equal(titleForLang(titles, "de"))
    languageValueWithLang(searchableArticle.title, "es") should equal(titleForLang(titles, "es"))
    languageValueWithLang(searchableArticle.title) should equal(titleForLang(titles))
  }

  private def verifyArticles(searchableArticle: SearchableArticle): Unit = {
    searchableArticle.content.languageValues.size should equal(articles.size)
    languageValueWithLang(searchableArticle.content, "nb") should equal(articleForLang(articles, "nb"))
    languageValueWithLang(searchableArticle.content, "nn") should equal(articleForLang(articles, "nn"))
    languageValueWithLang(searchableArticle.content, "en") should equal(articleForLang(articles, "en"))
    languageValueWithLang(searchableArticle.content, "fr") should equal(articleForLang(articles, "fr"))
    languageValueWithLang(searchableArticle.content, "de") should equal(articleForLang(articles, "de"))
    languageValueWithLang(searchableArticle.content, "es") should equal(articleForLang(articles, "es"))
    languageValueWithLang(searchableArticle.content) should equal(articleForLang(articles))
  }

  private def verifyTags(searchableArticle: SearchableArticle): Unit = {
    languageListWithLang(searchableArticle.tags, "nb") should equal(tagsForLang(articleTags, "nb"))
    languageListWithLang(searchableArticle.tags, "nn") should equal(tagsForLang(articleTags, "nn"))
    languageListWithLang(searchableArticle.tags, "en") should equal(tagsForLang(articleTags, "en"))
    languageListWithLang(searchableArticle.tags, "fr") should equal(tagsForLang(articleTags, "fr"))
    languageListWithLang(searchableArticle.tags, "de") should equal(tagsForLang(articleTags, "de"))
    languageListWithLang(searchableArticle.tags, "es") should equal(tagsForLang(articleTags, "es"))
    languageListWithLang(searchableArticle.tags) should equal(tagsForLang(articleTags))
  }

  private def languageValueWithLang(languageValues: SearchableLanguageValues, lang: String = "unknown"): String = {
    languageValues.languageValues.find(_.language == lang).get.value
  }

  private def languageListWithLang(languageList: SearchableLanguageList, lang: String = "unknown"): Seq[String] = {
    languageList.languageValues.find(_.language == lang).get.value
  }

  private def titleForLang(titles: Seq[Title], lang: String = "unknown"): String = {
    titles.find(_.language == lang).get.title
  }

  private def articleForLang(articles: Seq[ArticleContent], lang: String = "unknown"): String = {
    articles.find(_.language == lang).get.content
  }

  private def tagsForLang(tags: Seq[Tag], lang: String = "unknown") = {
    tags.find(_.language == lang).get.tags
  }
}
