/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi

import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.search.SearchSettings
import no.ndla.searchapi.model.taxonomy._
import org.joda.time.DateTime

object TestData {
  private val publicDomainCopyright= Copyright("publicdomain", "", List(), List(), List(), None, None, None)
  private val byNcSaCopyright = Copyright("by-nc-sa", "Gotham City", List(Author("Writer", "DC Comics")), List(), List(), None, None, None)
  private val copyrighted = Copyright("copyrighted", "New York", List(Author("Writer", "Clark Kent")), List(), List(), None, None, None)
  private val today = new DateTime().withMillisOfSecond(0)

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
    today.minusDays(4),
    today.minusDays(2),
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


  val article1 = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(ArticleTitle("Batmen er på vift med en bil", "nb")),
    introduction = List(ArticleIntroduction("Batmen", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(3)
  )

  val article2 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(2),
    title = List(ArticleTitle("Pingvinen er ute og går", "nb")),
    introduction = List(ArticleIntroduction("Pingvinen", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(2))
  val article3 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(3),
    title = List(ArticleTitle("Donald Duck kjører bil", "nb")),
    introduction = List(ArticleIntroduction("Donald Duck", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("and"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(1)
  )
  val article4 = TestData.sampleArticleWithCopyrighted.copy(
    id = Option(4),
    title = List(ArticleTitle("Superman er ute og flyr", "nb")),
    introduction = List(ArticleIntroduction("Superman", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("supermann"), "nb")),
    created = today.minusDays(4),
    updated = today
  )
  val article5 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(5),
    title = List(ArticleTitle("Hulken løfter biler", "nb")),
    introduction = List(ArticleIntroduction("Hulken", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("hulk"), "nb")),
    created = today.minusDays(40),
    updated = today.minusDays(35)
  )
  val article6 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(6),
    title = List(ArticleTitle("Loke og Tor prøver å fange midgaardsormen", "nb")),
    introduction = List(ArticleIntroduction("Loke og Tor", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("Loke", "Tor", "Naglfar"), "nb")),
    created = today.minusDays(30),
    updated = today.minusDays(25)
  )
  val article7 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(7),
    title = List(ArticleTitle("Yggdrasil livets tre", "nb")),
    introduction = List(ArticleIntroduction("Yggdrasil", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("yggdrasil"), "nb")),
    created = today.minusDays(20),
    updated = today.minusDays(15)
  )
  val article8 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(8),
    title = List(ArticleTitle("Baldur har mareritt", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle.toString
  )
  val article9 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(9),
    title = List(ArticleTitle("En Baldur har mareritt om Ragnarok", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle.toString
  )
  val article10 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(10),
    title = List(ArticleTitle("This article is in english", "en")),
    introduction = List(ArticleIntroduction("Engulsk", "en")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Something something <em>english</em> What", "en")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("englando"), "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle.toString
  )
  val article11 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(11),
    title = List(ArticleTitle("Katter", "nb"), ArticleTitle("Cats", "en")),
    introduction = List(ArticleIntroduction("Katter er store", "nb"), ArticleIntroduction("Cats are big", "en")),
    metaDescription = List(ArticleMetaDescription("hurr durr ima sheep", "en")),
    content = List(ArticleContent("<p>Noe om en katt</p>", "nb"), ArticleContent("<p>Something about a cat</p>", "en")),
    visualElement = List.empty,
    tags = List(ArticleTag(List("ikkehund"), "nb"), ArticleTag(List("notdog"), "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle.toString
  )

  val articlesToIndex: Seq[Article] = List(
    article1, article2, article3, article4, article5, article6, article7, article8, article9, article10, article11
  )

  val subjects = List(
    Resource("urn:subject:1", "Matte", None, "/subject:1"),
    Resource("urn:subject:2", "Historie", None, "/subject:2")
  )
  val filters = List(
    Filter("urn:filter:1", "VG1", "urn:subject:1"),
    Filter("urn:filter:2", "VG2", "urn:subject:1"),
    Filter("urn:filter:3", "VG3", "urn:subject:1"),
    Filter("urn:filter:4", "VG1", "urn:subject:2"),
    Filter("urn:filter:5", "VG2", "urn:subject:2"),
    Filter("urn:filter:6", "YF-VG1", "urn:subject:2"),
    Filter("urn:filter:7", "Tysk 2", "urn:subject:1")
  )
  val resourceFilterConnections = List(
    ResourceFilterConnection("urn:resource:1", "urn:filter:1", "urn:resource-filter:1", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:1", "urn:filter:2", "urn:resource-filter:2", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:1", "urn:filter:3", "urn:resource-filter:3", "urn:relevance:supplementary"),

    ResourceFilterConnection("urn:resource:3", "urn:filter:2", "urn:resource-filter:4", "urn:relevance:supplementary"),
    ResourceFilterConnection("urn:resource:3", "urn:filter:7", "urn:resource-filter:5", "urn:relevance:supplementary"),

    ResourceFilterConnection("urn:resource:4", "urn:filter:3", "urn:resource-filter:6", "urn:relevance:core"),

    ResourceFilterConnection("urn:resource:5", "urn:filter:2", "urn:resource-filter:7", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:5", "urn:filter:4", "urn:resource-filter:8", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:5", "urn:filter:5", "urn:resource-filter:9", "urn:relevance:core"),

    ResourceFilterConnection("urn:resource:6", "urn:filter:6", "urn:resource-filter:10", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:6", "urn:filter:5", "urn:resource-filter:11", "urn:relevance:core"),

    ResourceFilterConnection("urn:resource:7", "urn:filter:4", "urn:resource-filter:12", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:7", "urn:filter:6", "urn:resource-filter:13", "urn:relevance:core"),
  )
  val relevances = List(
    Relevance("urn:relevance:core", "Kjernestoff"),
    Relevance("urn:relevance:supplementary", "Tilleggsstoff")
  )
  val resourceTypes = List(
    ResourceType("urn:resourcetype:learningpath", "Læringssti", None),
    ResourceType("urn:resourcetype:subjectMaterial", "Fagstoff", Some(List(
      ResourceType("urn:resourcetype:academicArticle", "Fagartikkel", None),
      ResourceType("urn:resourcetype:guidance", "Veiledning", None)
    ))),
    ResourceType("urn:resourcetype:reviewResource", "Vurderingsressurs", Some(List(
      ResourceType("urn:resourcetype:teacherEvaluation", "Lærervurdering", None),
      ResourceType("urn:resourcetype:selfEvaluation", "Egenvurdering", None),
      ResourceType("urn:resourcetype:peerEvaluation", "Medelevvurdering", Some(List(
        ResourceType("urn:resourcetype:nested", "SuperNested ResourceType", None)
      )))
    )))
  )

  val resources = List(
    Resource("urn:resource:1", article1.title.head.title, Some(s"urn:article:${article1.id.get}"), s"/subject:1/topic:1/resource:1"),
    Resource("urn:resource:2", article2.title.head.title, Some(s"urn:article:${article2.id.get}"), s"/subject:1/topic:1/resource:2"),
    Resource("urn:resource:3", article3.title.head.title, Some(s"urn:article:${article3.id.get}"), s"/subject:1/topic:3/resource:3"),
    Resource("urn:resource:4", article4.title.head.title, Some(s"urn:article:${article4.id.get}"), s"/subject:1/topic:1/topic:2/resource:4"),
    Resource("urn:resource:5", article5.title.head.title, Some(s"urn:article:${article5.id.get}"), s"/subject:2/topic:4/resource:5"),
    Resource("urn:resource:6", article6.title.head.title, Some(s"urn:article:${article6.id.get}"), s"/subject:2/topic:4/resource:6"),
    Resource("urn:resource:7", article7.title.head.title, Some(s"urn:article:${article7.id.get}"), s"/subject:2/topic:4/resource:7")
  )
  val topics = List(
    Resource("urn:topic:1", article8.title.head.title, Some(s"urn:article:${article8.id.get}"), "/subject:1/topic:1"),
    Resource("urn:topic:2", article9.title.head.title, Some(s"urn:article:${article9.id.get}"), "/subject:1/topic:1/topic:2"),
    Resource("urn:topic:3", article10.title.head.title, Some(s"urn:article:${article10.id.get}"), "/subject:1/topic:3"),
    Resource("urn:topic:4", article11.title.head.title, Some(s"urn:article:${article11.id.get}"), "/subject:2/topic:4")
  )
  val subjectTopicConnections = List(
    SubjectTopicConnection("urn:subject:1", "urn:topic:1", "urn:subject-topic:1", primary = true, 1),
    SubjectTopicConnection("urn:subject:1", "urn:topic:3", "urn:subject-topic:2", primary = true, 1),
    SubjectTopicConnection("urn:subject:2", "urn:topic:4", "urn:subject-topic:3", primary = true, 1)
  )
  val topicResourceConnections = List(
    TopicResourceConnection("urn:topic:1", "urn:resource:1", "urn:topic-resource:1", primary = true, 1),
    TopicResourceConnection("urn:topic:4", "urn:resource:1", "urn:topic-resource:2", primary = true, 1),
    TopicResourceConnection("urn:topic:1", "urn:resource:2", "urn:topic-resource:3", primary = true, 1),
    TopicResourceConnection("urn:topic:3", "urn:resource:3", "urn:topic-resource:4", primary = true, 1),
    TopicResourceConnection("urn:topic:2", "urn:resource:4", "urn:topic-resource:5", primary = true, 1),
    TopicResourceConnection("urn:topic:4", "urn:resource:5", "urn:topic-resource:6", primary = true, 1),
    TopicResourceConnection("urn:topic:4", "urn:resource:6", "urn:topic-resource:7", primary = true, 1),
    TopicResourceConnection("urn:topic:4", "urn:resource:7", "urn:topic-resource:8", primary = true, 1),
    TopicResourceConnection("urn:topic:3", "urn:resource:5", "urn:topic-resource:9", primary = true, 1)
  )
  val topicSubtopicConnections = List(
    TopicSubtopicConnection("urn:topic:1", "urn:topic:2", "urn:topic-subtopic:1", primary = true, 1)
  )
  val resourceResourceTypeConnections = List(
    ResourceResourceTypeConnection("urn:resource:1", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:1"),
    ResourceResourceTypeConnection("urn:resource:2", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:2"),
    ResourceResourceTypeConnection("urn:resource:2", "urn:resourcetype:academicArticle", "urn:resource-resourcetype:3"),
    ResourceResourceTypeConnection("urn:resource:3", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:4"),
    ResourceResourceTypeConnection("urn:resource:4", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:5"),
    ResourceResourceTypeConnection("urn:resource:5", "urn:resourcetype:academicArticle", "urn:resource-resourcetype:6"),
    ResourceResourceTypeConnection("urn:resource:6", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:7"),
    ResourceResourceTypeConnection("urn:resource:7", "urn:resourcetype:guidance", "urn:resource-resourcetype:8"),
    ResourceResourceTypeConnection("urn:resource:7", "urn:resourcetype:nested", "urn:resource-resourcetype:9")
  )

  val taxonomyTestBundle = Bundle(
    filters = filters,
    relevances = relevances,
    resourceFilterConnections = resourceFilterConnections,
    resourceResourceTypeConnections = resourceResourceTypeConnections,
    resourceTypes = resourceTypes,
    resources = resources,
    subjectTopicConnections = subjectTopicConnections,
    subjects = subjects,
    topicFilterConnections = List.empty, //TODO: maybe connect filters to topics? This is always empty in taxonomy-Test
    topicResourceConnections = topicResourceConnections,
    topicSubtopicConnections = topicSubtopicConnections,
    topics = topics
  )

  val searchSettings = SearchSettings(
    fallback = false,
    language = Language.DefaultLanguage,
    license = None,
    page = 1,
    pageSize = 10,
    sort = Sort.ByIdAsc,
    withIdIn = List.empty,
    taxonomyFilters = List.empty,
    subjects = List.empty,
    resourceTypes = List.empty,
    contextTypes = List.empty,
    supportedLanguages = List.empty
  )
}
