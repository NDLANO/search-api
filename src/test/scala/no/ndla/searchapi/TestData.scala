/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi

import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.draft.{ArticleStatus, Draft}
import no.ndla.searchapi.model.domain.learningpath.{
  Description,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus
}
import no.ndla.searchapi.model.search._
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.model.taxonomy._
import org.joda.time.DateTime

object TestData {
  private val publicDomainCopyright = Copyright("publicdomain", "", List(), List(), List(), None, None, None)
  private val byNcSaCopyright =
    Copyright("by-nc-sa", "Gotham City", List(Author("Writer", "DC Comics")), List(), List(), None, None, None)
  private val copyrighted =
    Copyright("copyrighted", "New York", List(Author("Writer", "Clark Kent")), List(), List(), None, None, None)
  val today: DateTime = new DateTime().withMillisOfSecond(0)

  val sampleArticleTitle = ArticleApiTitle("tittell", "nb")
  val sampleArticleVisualElement = ArticleApiVisualElement("""<embed data-resource="image">""", "nb")
  val sampleArticleIntro = ArticleApiIntro("intro", "nb")

  val sampleArticleSearch = ArticleApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      ArticleApiSearchResult(1,
                             sampleArticleTitle,
                             Option(sampleArticleVisualElement),
                             Option(sampleArticleIntro),
                             "http://articles/1",
                             "by",
                             "standard",
                             Seq("nb", "en")),
      ArticleApiSearchResult(
        2,
        ArticleApiTitle("Another title", "nb"),
        Option(sampleArticleVisualElement),
        Option(sampleArticleIntro),
        "http://articles/2",
        "by",
        "standard",
        Seq("nb", "en")
      )
    )
  )

  val sampleImageSearch = ImageApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      ImageApiSearchResult("1",
                           ImageTitle("title", "en"),
                           ImageAltText("alt text", "en"),
                           "http://images/1.jpg",
                           "http://images/1",
                           "by",
                           Seq("en")),
      ImageApiSearchResult("1",
                           ImageTitle("title", "en"),
                           ImageAltText("alt text", "en"),
                           "http://images/1.jpg",
                           "http://images/1",
                           "by",
                           Seq("en"))
    )
  )

  val sampleLearningpath = LearningpathApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      LearningpathApiSearchResult(
        1,
        LearningpathApiTitle("en title", "nb"),
        LearningpathApiDescription("en description", "nb"),
        LearningpathApiIntro("intro", "nb"),
        "http://learningpath/1",
        None,
        None,
        "PUBLISHED",
        "2016-07-06T09:08:08Z",
        LearningPathApiTags(Seq(), "nb"),
        Seq("nb"),
        None
      ),
      LearningpathApiSearchResult(
        2,
        LearningpathApiTitle("en annen titlel", "nb"),
        LearningpathApiDescription("beskrivelse", "nb"),
        LearningpathApiIntro("itroduksjon", "nb"),
        "http://learningpath/2",
        None,
        None,
        "PUBLISHED",
        "2016-07-06T09:08:08Z",
        LearningPathApiTags(Seq(), "nb"),
        Seq("nb"),
        None
      ),
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
    Seq(Title("test", "en")),
    Seq(ArticleContent("<section><div>test</div></section>", "en")),
    publicDomainCopyright,
    Seq(),
    Seq(),
    Seq(VisualElement("image", "en")),
    Seq(ArticleIntroduction("This is an introduction", "en")),
    Seq(MetaDescription("meta", "en")),
    Seq(),
    today.minusDays(4),
    today.minusDays(2),
    "ndalId54321",
    LearningResourceType.Article
  )

  val sampleDomainArticle = Article(
    Option(articleId),
    Option(2),
    Seq(Title("title", "nb")),
    Seq(ArticleContent("content", "nb")),
    Copyright("by", "", Seq(), Seq(), Seq(), None, None, None),
    Seq(Tag(Seq("tag"), "nb")),
    Seq(),
    Seq(),
    Seq(),
    Seq(MetaDescription("meta description", "nb")),
    Seq(ArticleMetaImage("11", "alt", "nb")),
    today,
    today,
    "ndalId54321",
    LearningResourceType.Article
  )

  val sampleDomainArticle2 = Article(
    None,
    None,
    Seq(Title("test", "en")),
    Seq(ArticleContent("<article><div>test</div></article>", "en")),
    Copyright("publicdomain", "", Seq(), Seq(), Seq(), None, None, None),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    today,
    today,
    "ndalId54321",
    LearningResourceType.Article
  )
  val sampleArticleWithByNcSa: Article = sampleArticleWithPublicDomain.copy(copyright = byNcSaCopyright)
  val sampleArticleWithCopyrighted: Article = sampleArticleWithPublicDomain.copy(copyright = copyrighted)

  val article1 = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(Title("Batmen er på vift med en bil", "nb")),
    introduction = List(ArticleIntroduction("Batmen", "nb")),
    metaDescription = List.empty,
    content = List(
      ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(3),
    copyright = byNcSaCopyright.copy(creators = List(Author("Forfatter", "Kjekspolitiet")))
  )

  val article2 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(2),
    title = List(Title("Pingvinen er ute og går", "nb")),
    introduction = List(ArticleIntroduction("Pingvinen", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(2),
    copyright = publicDomainCopyright.copy(creators = List(Author("Forfatter", "Pjolter")),
                                           processors = List(Author("Editorial", "Svims")))
  )

  val article3 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(3),
    title = List(Title("Donald Duck kjører bil", "nb")),
    introduction = List(ArticleIntroduction("Donald Duck", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("and"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(1)
  )

  val article4 = TestData.sampleArticleWithCopyrighted.copy(
    id = Option(4),
    title = List(Title("Superman er ute og flyr", "nb")),
    introduction = List(ArticleIntroduction("Superman", "nb")),
    metaDescription = List.empty,
    content =
      List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("supermann"), "nb")),
    created = today.minusDays(4),
    updated = today
  )

  val article5 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(5),
    title = List(Title("Hulken løfter biler", "nb")),
    introduction = List(ArticleIntroduction("Hulken", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("hulk"), "nb")),
    created = today.minusDays(40),
    updated = today.minusDays(35)
  )

  val article6 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(6),
    title = List(Title("Loke og Tor prøver å fange midgaardsormen", "nb")),
    introduction = List(ArticleIntroduction("Loke og Tor", "nb")),
    metaDescription = List.empty,
    content = List(
      ArticleContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>",
                     "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("Loke", "Tor", "Naglfar"), "nb")),
    created = today.minusDays(30),
    updated = today.minusDays(25)
  )

  val article7 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(7),
    title = List(Title("Yggdrasil livets tre", "nb")),
    introduction = List(ArticleIntroduction("Yggdrasil", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("yggdrasil"), "nb")),
    created = today.minusDays(20),
    updated = today.minusDays(15)
  )

  val article8 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(8),
    title = List(Title("Baldur har mareritt", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle
  )

  val article9 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(9),
    title = List(Title("En Baldur har mareritt om Ragnarok", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle
  )

  val article10 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(10),
    title = List(Title("This article is in english", "en")),
    introduction = List(ArticleIntroduction("Engulsk", "en")),
    metaDescription = List.empty,
    content = List(ArticleContent("<p>Something something <em>english</em> What", "en")),
    metaImage = List(ArticleMetaImage("442", "alt", "en")),
    visualElement = List.empty,
    tags = List(Tag(List("englando"), "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle
  )

  val article11 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(11),
    title = List(Title("Katter", "nb"), Title("Cats", "en")),
    introduction = List(ArticleIntroduction("Katter er store", "nb"), ArticleIntroduction("Cats are big", "en")),
    metaDescription = List(MetaDescription("hurr durr ima sheep", "en")),
    content = List(ArticleContent("<p>Noe om en katt</p>", "nb"), ArticleContent("<p>Something about a cat</p>", "en")),
    visualElement = List.empty,
    tags = List(Tag(List("ikkehund"), "nb"), Tag(List("notdog"), "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle
  )

  val articlesToIndex: Seq[Article] = List(
    article1,
    article2,
    article3,
    article4,
    article5,
    article6,
    article7,
    article8,
    article9,
    article10,
    article11
  )

  val draftPublicDomainCopyright =
    draft.Copyright(Some("publicdomain"), Some(""), List.empty, List(), List(), None, None, None)

  val draftByNcSaCopyright = draft.Copyright(Some("by-nc-sa"),
                                             Some("Gotham City"),
                                             List(Author("Forfatter", "DC Comics")),
                                             List(),
                                             List(),
                                             None,
                                             None,
                                             None)

  val draftCopyrighted = draft.Copyright(Some("copyrighted"),
                                         Some("New York"),
                                         List(Author("Forfatter", "Clark Kent")),
                                         List(),
                                         List(),
                                         None,
                                         None,
                                         None)

  val sampleDraftWithPublicDomain = Draft(
    Option(1),
    Option(1),
    ArticleStatus.ValueSet(ArticleStatus.DRAFT),
    Seq(Title("test", "en")),
    Seq(ArticleContent("<section><div>test</div></section>", "en")),
    Some(draftPublicDomainCopyright),
    Seq.empty,
    Seq.empty,
    Seq(VisualElement("image", "en")),
    Seq(ArticleIntroduction("This is an introduction", "en")),
    Seq(MetaDescription("meta", "en")),
    Seq.empty,
    DateTime.now().minusDays(4),
    DateTime.now().minusDays(2),
    "ndalId54321",
    LearningResourceType.Article,
    List.empty
  )

  val sampleDraftWithByNcSa: Draft = sampleDraftWithPublicDomain.copy(copyright = Some(draftByNcSaCopyright))
  val sampleDraftWithCopyrighted: Draft = sampleDraftWithPublicDomain.copy(copyright = Some(draftCopyrighted))

  val draft1 = TestData.sampleDraftWithByNcSa.copy(
    id = Option(1),
    title = List(Title("Batmen er på vift med en bil", "nb")),
    introduction = List(ArticleIntroduction("Batmen", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(
      ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")),
    tags = List(Tag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(3),
    copyright = Some(draftByNcSaCopyright.copy(creators = List(Author("Forfatter", "Kjekspolitiet"))))
  )

  val draft2 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(2),
    title = List(Title("Pingvinen er ute og går", "nb")),
    introduction = List(ArticleIntroduction("Pingvinen", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
    tags = List(Tag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(2),
    copyright = Some(
      draftPublicDomainCopyright.copy(creators = List(Author("Forfatter", "Pjolter")),
                                      processors = List(Author("Editorial", "Svims"))))
  )

  val draft3 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(3),
    title = List(Title("Donald Duck kjører bil", "nb")),
    introduction = List(ArticleIntroduction("Donald Duck", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
    tags = List(Tag(List("and"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(1)
  )

  val draft4 = TestData.sampleDraftWithCopyrighted.copy(
    id = Option(4),
    title = List(Title("Superman er ute og flyr", "nb")),
    introduction = List(ArticleIntroduction("Superman", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content =
      List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
    tags = List(Tag(List("supermann"), "nb")),
    created = today.minusDays(4),
    updated = today
  )

  val draft5 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(5),
    title = List(Title("Hulken løfter biler", "nb")),
    introduction = List(ArticleIntroduction("Hulken", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
    tags = List(Tag(List("hulk"), "nb")),
    created = today.minusDays(40),
    updated = today.minusDays(35),
    notes = List("kakemonster")
  )

  val draft6 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(6),
    title = List(Title("Loke og Tor prøver å fange midgaardsormen", "nb")),
    introduction = List(ArticleIntroduction("Loke og Tor", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(
      ArticleContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>",
                     "nb")),
    tags = List(Tag(List("Loke", "Tor", "Naglfar"), "nb")),
    created = today.minusDays(30),
    updated = today.minusDays(25)
  )

  val draft7 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(7),
    title = List(Title("Yggdrasil livets tre", "nb")),
    introduction = List(ArticleIntroduction("Yggdrasil", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
    tags = List(Tag(List("yggdrasil"), "nb")),
    created = today.minusDays(20),
    updated = today.minusDays(15)
  )

  val draft8 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(8),
    title = List(Title("Baldur har mareritt", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    tags = List(Tag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle
  )

  val draft9 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(9),
    title = List(Title("Baldur har mareritt om Ragnarok", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    tags = List(Tag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle
  )

  val draft10 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(10),
    title = List(Title("This article is in english", "en")),
    introduction = List(ArticleIntroduction("Engulsk", "en")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Something something <em>english</em> What", "en")),
    tags = List(Tag(List("englando"), "en")),
    metaImage = List(ArticleMetaImage("123", "alt", "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle
  )

  val draft11 = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(11),
    title = List(Title("Katter", "nb"), Title("Cats", "en")),
    introduction = List(ArticleIntroduction("Katter er store", "nb"), ArticleIntroduction("Cats are big", "en")),
    content = List(ArticleContent("<p>Noe om en katt</p>", "nb"), ArticleContent("<p>Something about a cat</p>", "en")),
    tags = List(Tag(List("katt"), "nb"), Tag(List("cat"), "en")),
    metaDescription = List(MetaDescription("hurr dirr ima sheep", "en")),
    visualElement = List.empty,
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = LearningResourceType.TopicArticle
  )

  val draftsToIndex: List[Draft] = List(
    draft1,
    draft2,
    draft3,
    draft4,
    draft5,
    draft6,
    draft7,
    draft8,
    draft9,
    draft10,
    draft11
  )

  val paul = Author("author", "Truly Weird Rand Paul")
  val license = "publicdomain"
  val copyright = domain.learningpath.Copyright(license, List(paul))

  val DefaultLearningPath = LearningPath(
    id = None,
    revision = None,
    externalId = None,
    isBasedOn = None,
    title = List(),
    description = List(),
    coverPhotoId = None,
    duration = Some(0),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    lastUpdated = today,
    tags = List(),
    owner = "owner",
    copyright = copyright
  )

  val PenguinId = 1
  val BatmanId = 2
  val DonaldId = 3
  val UnrelatedId = 4
  val EnglandoId = 5
  val KekId = 6

  val learningPath1 = DefaultLearningPath.copy(
    id = Some(PenguinId),
    title = List(Title("Pingvinen er en kjeltring", "nb")),
    description = List(Description("Dette handler om fugler", "nb")),
    duration = Some(1),
    lastUpdated = today.minusDays(34),
    tags = List(Tag(List("superhelt", "kanikkefly"), "nb"))
  )

  val learningPath2 = DefaultLearningPath.copy(
    id = Some(BatmanId),
    title = List(Title("Batman er en tøff og morsom helt", "nb"), Title("Batman is a tough guy", "en")),
    description = List(Description("Dette handler om flaggermus, som kan ligne litt på en fugl", "nb")),
    duration = Some(2),
    lastUpdated = today.minusDays(3),
    tags = List(Tag(Seq("superhelt", "kanfly"), "nb"))
  )

  val learningPath3 = DefaultLearningPath.copy(
    id = Some(DonaldId),
    title = List(Title("Donald er en tøff, rar og morsom and", "nb"), Title("Donald is a weird duck", "en")),
    description = List(Description("Dette handler om en and, som også minner om både flaggermus og fugler.", "nb")),
    duration = Some(3),
    lastUpdated = today.minusDays(4),
    tags = List(Tag(Seq("disney", "kanfly"), "nb"))
  )

  val learningPath4 = DefaultLearningPath.copy(
    id = Some(UnrelatedId),
    title = List(Title("Unrelated", "en"), Title("Urelatert", "nb")),
    description = List(Description("This is unrelated", "en"), Description("Dette er en urelatert", "nb")),
    duration = Some(4),
    lastUpdated = today.minusDays(5),
    tags = List()
  )

  val learningPath5 = DefaultLearningPath.copy(
    id = Some(EnglandoId),
    title = List(Title("Englando", "en")),
    description = List(Description("This is a englando learningpath", "en")),
    duration = Some(5),
    lastUpdated = today.minusDays(6),
    tags = List(),
    copyright = copyright.copy(contributors = List(Author("Writer", "Svims")))
  )

  val learningPath6 = DefaultLearningPath.copy(
    id = Some(KekId),
    title = List(Title("Kek", "en")),
    description = List(Description("This is kek", "en")),
    duration = Some(5),
    lastUpdated = today.minusDays(7),
    tags = List()
  )

  val learningPathsToIndex: List[LearningPath] = List(
    learningPath1,
    learningPath2,
    learningPath3,
    learningPath4,
    learningPath5,
    learningPath6
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
    ResourceType(
      "urn:resourcetype:subjectMaterial",
      "Fagstoff",
      Some(
        List(
          ResourceType("urn:resourcetype:academicArticle", "Fagartikkel", None),
          ResourceType("urn:resourcetype:guidance", "Veiledning", None)
        ))
    ),
    ResourceType(
      "urn:resourcetype:reviewResource",
      "Vurderingsressurs",
      Some(
        List(
          ResourceType("urn:resourcetype:teacherEvaluation", "Lærervurdering", None),
          ResourceType("urn:resourcetype:selfEvaluation", "Egenvurdering", None),
          ResourceType("urn:resourcetype:peerEvaluation",
                       "Medelevvurdering",
                       Some(
                         List(
                           ResourceType("urn:resourcetype:nested", "SuperNested ResourceType", None)
                         )))
        ))
    )
  )

  val resources = List(
    Resource("urn:resource:1",
             article1.title.head.title,
             Some(s"urn:article:${article1.id.get}"),
             s"/subject:1/topic:1/resource:1"),
    Resource("urn:resource:2",
             article2.title.head.title,
             Some(s"urn:article:${article2.id.get}"),
             s"/subject:1/topic:1/resource:2"),
    Resource("urn:resource:3",
             article3.title.head.title,
             Some(s"urn:article:${article3.id.get}"),
             s"/subject:1/topic:3/resource:3"),
    Resource("urn:resource:4",
             article4.title.head.title,
             Some(s"urn:article:${article4.id.get}"),
             s"/subject:1/topic:1/topic:2/resource:4"),
    Resource("urn:resource:5",
             article5.title.head.title,
             Some(s"urn:article:${article5.id.get}"),
             s"/subject:2/topic:4/resource:5"),
    Resource("urn:resource:6",
             article6.title.head.title,
             Some(s"urn:article:${article6.id.get}"),
             s"/subject:2/topic:4/resource:6"),
    Resource("urn:resource:7",
             article7.title.head.title,
             Some(s"urn:article:${article7.id.get}"),
             s"/subject:2/topic:4/resource:7"),
    Resource("urn:resource:8",
             learningPath1.title.head.title,
             Some(s"urn:learningpath:${learningPath1.id.get}"),
             s"/subject:1/topic:1/resource:1"),
    Resource("urn:resource:9",
             learningPath2.title.head.title,
             Some(s"urn:learningpath:${learningPath2.id.get}"),
             s"/subject:1/topic:1/resource:2"),
    Resource("urn:resource:10",
             learningPath3.title.head.title,
             Some(s"urn:learningpath:${learningPath3.id.get}"),
             s"/subject:1/topic:3/resource:3"),
    Resource("urn:resource:11",
             learningPath4.title.head.title,
             Some(s"urn:learningpath:${learningPath4.id.get}"),
             s"/subject:1/topic:1/topic:2/resource:4"),
    Resource("urn:resource:12",
             learningPath5.title.head.title,
             Some(s"urn:learningpath:${learningPath5.id.get}"),
             s"/subject:2/topic:4/resource:5")
  )

  val topics = List(
    Resource("urn:topic:1", article8.title.head.title, Some(s"urn:article:${article8.id.get}"), "/subject:1/topic:1"),
    Resource("urn:topic:2",
             article9.title.head.title,
             Some(s"urn:article:${article9.id.get}"),
             "/subject:1/topic:1/topic:2"),
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
    TopicResourceConnection("urn:topic:3", "urn:resource:5", "urn:topic-resource:9", primary = true, 1),
    TopicResourceConnection("urn:topic:1", "urn:resource:8", "urn:topic-resource:10", primary = true, 1),
    TopicResourceConnection("urn:topic:1", "urn:resource:9", "urn:topic-resource:11", primary = true, 1),
    TopicResourceConnection("urn:topic:3", "urn:resource:10", "urn:topic-resource:12", primary = true, 1),
    TopicResourceConnection("urn:topic:2", "urn:resource:11", "urn:topic-resource:13", primary = true, 1),
    TopicResourceConnection("urn:topic:4", "urn:resource:12", "urn:topic-resource:14", primary = true, 1)
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
    ResourceResourceTypeConnection("urn:resource:7", "urn:resourcetype:nested", "urn:resource-resourcetype:9"),
    ResourceResourceTypeConnection("urn:resource:8", "urn:resourcetype:learningpath", "urn:resource-resourcetype:10"),
    ResourceResourceTypeConnection("urn:resource:9", "urn:resourcetype:learningpath", "urn:resource-resourcetype:11"),
    ResourceResourceTypeConnection("urn:resource:10", "urn:resourcetype:learningpath", "urn:resource-resourcetype:12"),
    ResourceResourceTypeConnection("urn:resource:11", "urn:resourcetype:learningpath", "urn:resource-resourcetype:13"),
    ResourceResourceTypeConnection("urn:resource:12", "urn:resourcetype:learningpath", "urn:resource-resourcetype:14"),
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
    topicFilterConnections = List.empty,
    topicResourceConnections = topicResourceConnections,
    topicSubtopicConnections = topicSubtopicConnections,
    topics = topics
  )

  val searchSettings = SearchSettings(
    fallback = false,
    language = Language.DefaultLanguage,
    license = None,
    page = 1,
    pageSize = 20,
    sort = Sort.ByIdAsc,
    withIdIn = List.empty,
    taxonomyFilters = List.empty,
    subjects = List.empty,
    resourceTypes = List.empty,
    learningResourceTypes = List.empty,
    supportedLanguages = List.empty
  )

  val multiDraftSearchSettings = MultiDraftSearchSettings(
    query = None,
    noteQuery = None,
    fallback = false,
    language = Language.DefaultLanguage,
    license = None,
    page = 1,
    pageSize = 20,
    sort = Sort.ByIdAsc,
    withIdIn = List.empty,
    taxonomyFilters = List.empty,
    subjects = List.empty,
    topics = List.empty,
    resourceTypes = List.empty,
    learningResourceTypes = List.empty,
    supportedLanguages = List.empty
  )

  val searchableResourceTypes = List(
    SearchableTaxonomyResourceType("urn:resourcetype:subjectMaterial",
                                   SearchableLanguageValues(Seq(LanguageValue("nb", "Fagstoff")))),
    SearchableTaxonomyResourceType("urn:resourcetype:academicArticle",
                                   SearchableLanguageValues(Seq(LanguageValue("nb", "Fagartikkel"))))
  )

  val searchableTaxonomyContexts = List(
    SearchableTaxonomyContext(
      id = "urn:resource:101",
      subjectId = "urn:subject:1",
      subject = SearchableLanguageValues(Seq(LanguageValue("nb", "Matte"))),
      path = "/subject:3/topic:1/topic:151/resource:101",
      breadcrumbs = SearchableLanguageList(
        Seq(
          LanguageValue("nb", Seq("Matte", "Østen for solen", "Vesten for månen"))
        )),
      contextType = LearningResourceType.Article.toString,
      filters = List(
        SearchableTaxonomyFilter(
          filterId = "urn:filter:1",
          name = SearchableLanguageValues(Seq(LanguageValue("nb", "VG1"))),
          relevance = SearchableLanguageValues(Seq(LanguageValue("nb", "Kjernestoff")))
        )),
      resourceTypes = searchableResourceTypes,
      parentTopicIds = List("urn:topic:1")
    )
  )
}
