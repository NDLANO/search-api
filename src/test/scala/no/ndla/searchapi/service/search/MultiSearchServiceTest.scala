/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */


package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import no.ndla.searchapi.SearchApiProperties.DefaultPageSize
import no.ndla.searchapi.integration.Elastic4sClientFactory
import no.ndla.searchapi.model.api.ApiTaxonomyContext
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.{Language, SearchableTaxonomyContext, Sort}
import no.ndla.searchapi.model.search.SearchSettings
import no.ndla.searchapi.model.taxonomy._
import no.ndla.searchapi.{SearchApiProperties, TestData, TestEnvironment, UnitSuite}
import no.ndla.tag.IntegrationTest
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.collection.immutable
import scala.util.Success

@IntegrationTest
class MultiSearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val e4sClient = Elastic4sClientFactory.getClient(searchServer = s"http://localhost:$esPort")

  override val multiSearchService = new MultiSearchService
  override val articleIndexService = new ArticleIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright("by-nc-sa", "Gotham City", List(Author("Forfatter", "DC Comics")), List(), List(), None, None, None)
  val publicDomain = Copyright("publicdomain", "Metropolis", List(Author("Forfatter", "Bruce Wayne")), List(), List(), None, None, None)
  val copyrighted = Copyright("copyrighted", "New York", List(Author("Forfatter", "Clark Kent")), List(), List(), None, None, None)

  val today = DateTime.now()


  val article1 = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(ArticleTitle("Batmen er på vift med en bil", "nb")),
    introduction = List(ArticleIntroduction("Batmen", "nb")),
    content = List(ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")),
    tags = List(ArticleTag(List("fugl"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(3).toDate
  )

  val article2 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(2),
    title = List(ArticleTitle("Pingvinen er ute og går", "nb")),
    introduction = List(ArticleIntroduction("Pingvinen", "nb")),
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
    tags = List(ArticleTag(List("fugl"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(2).toDate)
  val article3 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(3),
    title = List(ArticleTitle("Donald Duck kjører bil", "nb")),
    introduction = List(ArticleIntroduction("Donald Duck", "nb")),
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
    tags = List(ArticleTag(List("and"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.minusDays(1).toDate
  )
  val article4 = TestData.sampleArticleWithCopyrighted.copy(
    id = Option(4),
    title = List(ArticleTitle("Superman er ute og flyr", "nb")),
    introduction = List(ArticleIntroduction("Superman", "nb")),
    content = List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
    tags = List(ArticleTag(List("supermann"), "nb")),
    created = today.minusDays(4).toDate,
    updated = today.toDate
  )
  val article5 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(5),
    title = List(ArticleTitle("Hulken løfter biler", "nb")),
    introduction = List(ArticleIntroduction("Hulken", "nb")),
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
    tags = List(ArticleTag(List("hulk"), "nb")),
    created = today.minusDays(40).toDate,
    updated = today.minusDays(35).toDate
  )
  val article6 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(6),
    title = List(ArticleTitle("Loke og Tor prøver å fange midgaardsormen", "nb")),
    introduction = List(ArticleIntroduction("Loke og Tor", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>", "nb")),
    tags = List(ArticleTag(List("Loke", "Tor", "Naglfar"), "nb")),
    created = today.minusDays(30).toDate,
    updated = today.minusDays(25).toDate
  )
  val article7 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(7),
    title = List(ArticleTitle("Yggdrasil livets tre", "nb")),
    introduction = List(ArticleIntroduction("Yggdrasil", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
    tags = List(ArticleTag(List("yggdrasil"), "nb")),
    created = today.minusDays(20).toDate,
    updated = today.minusDays(15).toDate
  )
  val article8 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(8),
    title = List(ArticleTitle("Baldur har mareritt", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    tags = List(ArticleTag(List("baldur"), "nb")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = LearningResourceType.TopicArticle.toString
  )
  val article9 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(9),
    title = List(ArticleTitle("En Baldur har mareritt om Ragnarok", "nb")),
    introduction = List(ArticleIntroduction("Baldur", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    tags = List(ArticleTag(List("baldur"), "nb")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = LearningResourceType.TopicArticle.toString
  )
  val article10 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(10),
    title = List(ArticleTitle("This article is in english", "en")),
    introduction = List(ArticleIntroduction("Engulsk", "en")),
    content = List(ArticleContent("<p>Something something <em>english</em> What", "en")),
    tags = List(ArticleTag(List("englando"), "en")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
    articleType = LearningResourceType.TopicArticle.toString
  )
  val article11 = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(11),
    title = List(ArticleTitle("Katter", "nb"), ArticleTitle("Cats", "en")),
    introduction = List(ArticleIntroduction("Katter er store", "nb"), ArticleIntroduction("Cats are big", "en")),
    metaDescription = List(ArticleMetaDescription("hurr durr ima sheep", "en")),
    content = List(ArticleContent("<p>Noe om en katt</p>", "nb"), ArticleContent("<p>Something about a cat</p>", "en")),
    tags = List(ArticleTag(List("ikkehund"), "nb"), ArticleTag(List("notdog"), "en")),
    created = today.minusDays(10).toDate,
    updated = today.minusDays(5).toDate,
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
    Filter("urn:filter:6", "YF-VG1", "urn:subject:2")
  )
  val resourceFilterConnections = List(
    ResourceFilterConnection("urn:resource:1", "urn:filter:1", "urn:resource-filter:1", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:1", "urn:filter:2", "urn:resource-filter:1", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:1", "urn:filter:3", "urn:resource-filter:1", "urn:relevance:supplementary"),
    ResourceFilterConnection("urn:resource:3", "urn:filter:2", "urn:resource-filter:3", "urn:relevance:supplementary"),
    ResourceFilterConnection("urn:resource:4", "urn:filter:3", "urn:resource-filter:4", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:5", "urn:filter:4", "urn:resource-filter:5", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:5", "urn:filter:5", "urn:resource-filter:5", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:6", "urn:filter:6", "urn:resource-filter:5", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:6", "urn:filter:5", "urn:resource-filter:5", "urn:relevance:core"),
    ResourceFilterConnection("urn:resource:7", "urn:filter:6", "urn:resource-filter:5", "urn:relevance:core"),
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
    SubjectTopicConnection("urn:subject:1", "urn:topic:1", "urn:subject-topic:1", true, 1),
    SubjectTopicConnection("urn:subject:1", "urn:topic:3", "urn:subject-topic:2", true, 1),
    SubjectTopicConnection("urn:subject:2", "urn:topic:4", "urn:subject-topic:3", true, 1)
  )
  val topicResourceConnections = List(
    TopicResourceConnection("urn:topic:1", "urn:resource:1", "urn:topic-resource:1", true, 1),
    TopicResourceConnection("urn:topic:1", "urn:resource:2", "urn:topic-resource:2", true, 1),
    TopicResourceConnection("urn:topic:3", "urn:resource:3", "urn:topic-resource:3", true, 1),
    TopicResourceConnection("urn:topic:2", "urn:resource:4", "urn:topic-resource:4", true, 1),
    TopicResourceConnection("urn:topic:4", "urn:resource:5", "urn:topic-resource:5", true, 1),
    TopicResourceConnection("urn:topic:4", "urn:resource:6", "urn:topic-resource:6", true, 1),
    TopicResourceConnection("urn:topic:4", "urn:resource:7", "urn:topic-resource:7", true, 1)
  )
  val topicSubtopicConnections = List(
    TopicSubtopicConnection("urn:topic:1", "urn:topic:2", "urn:topic-subtopic:1", true, 1)
  )
  val resourceResourceTypeConnections = List(
    ResourceResourceTypeConnection("urn:resource:1", "urn:subjectMaterial", "urn:resource-resourcetype:1"),
    ResourceResourceTypeConnection("urn:resource:2", "urn:subjectMaterial", "urn:resource-resourcetype:2"),
    ResourceResourceTypeConnection("urn:resource:2", "urn:academicArticle", "urn:resource-resourcetype:3"),
    ResourceResourceTypeConnection("urn:resource:3", "urn:subjectMaterial", "urn:resource-resourcetype:4"),
    ResourceResourceTypeConnection("urn:resource:4", "urn:subjectMaterial", "urn:resource-resourcetype:5"),
    ResourceResourceTypeConnection("urn:resource:5", "urn:academicArticle", "urn:resource-resourcetype:6"),
    ResourceResourceTypeConnection("urn:resource:6", "urn:subjectMaterial", "urn:resource-resourcetype:7"),
    ResourceResourceTypeConnection("urn:resource:7", "urn:guidance", "urn:resource-resourcetype:8")
  )

  val taxonomyTestBundle = Bundle(
    filters = filters,
    relevances = relevances,
    resourceFilterConnections = List.empty, // TODO: connect filters to some resources pls
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
    types = List.empty,
    withIdIn = List.empty,
    taxonomyFilters = List.empty
  )

  override def beforeAll = {
    articleIndexService.createIndexWithName(SearchApiProperties.SearchIndexes("articles"))

    articlesToIndex.map(article =>
      articleIndexService.indexDocument(article, Some(taxonomyTestBundle))
    )

    blockUntil(() => multiSearchService.countDocuments == articlesToIndex.size)
  }

  private def deleteIndexesThatStartWith(startsWith: String): Unit = {
    val Success(result) = e4sClient.execute(getAliases())
    val toDelete = result.result.mappings.filter(_._1.name.startsWith(startsWith)).map(_._1.name)

    if(toDelete.nonEmpty) {
      e4sClient.execute(deleteIndex(toDelete))
    }
  }

  override def afterAll() = {
    deleteIndexesThatStartWith(SearchApiProperties.SearchIndexes("articles"))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    multiSearchService.getStartAtAndNumResults(0, 1000) should equal((0, SearchApiProperties.MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("all should return only articles of a given type if a type filter is specified") {
    val Success(results) = multiSearchService.all(searchSettings.copy(types = List(LearningResourceType.TopicArticle.toString)))
    results.totalCount should be(3)

    val Success(results2) = multiSearchService.all(searchSettings.copy(types = LearningResourceType.all))
    results2.totalCount should be(9)
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByIdAsc))
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(1)
    hits(1).id should be(2)
    hits(2).id should be(3)
    hits(3).id should be(5)
    hits(4).id should be(6)
    hits(5).id should be(7)
    hits.last.id should be(11)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByIdDesc))
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be (11)
    hits.last.id should be (1)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(8)
    hits(1).id should be(1)
    hits(2).id should be(3)
    hits(3).id should be(9)
    hits(4).id should be(5)
    hits(5).id should be(11)
    hits(6).id should be(6)
    hits(7).id should be(2)
    hits.last.id should be(7)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByTitleDesc))
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(7)
    hits(1).id should be(2)
    hits(2).id should be(6)
    hits(3).id should be(11)
    hits(4).id should be(5)
    hits(5).id should be(9)
    hits(6).id should be(3)
    hits(7).id should be(1)
    hits.last.id should be(8)
  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByLastUpdatedDesc))
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(3)
    hits.last.id should be(5)
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByLastUpdatedAsc))
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(5)
    hits(1).id should be(6)
    hits(2).id should be(7)
    hits(3).id should be(8)
    hits(4).id should be(9)
    hits(5).id should be(11)
    hits(6).id should be(1)
    hits(7).id should be(2)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) = multiSearchService.all(searchSettings.copy(page = 1, pageSize = 2, sort = Sort.ByTitleAsc))
    val Success(page2) = multiSearchService.all(searchSettings.copy(page = 2, pageSize = 2, sort = Sort.ByTitleAsc))
    val hits1 = page1.results
    val hits2 = page2.results
    page1.totalCount should be(9)
    page1.page should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(1)
    page2.totalCount should be(9)
    page2.page should be(2)
    hits2.size should be(2)
    hits2.head.id should be(3)
    hits2.last.id should be(9)
  }

  test("matchingQuery should filter results based on an article type filter") {
    val Success(results) = multiSearchService.matchingQuery("bil", searchSettings.copy(sort = Sort.ByRelevanceDesc, types = List(LearningResourceType.TopicArticle.toString)))
    results.totalCount should be(0)

    val Success(results2) = multiSearchService.matchingQuery("bil", searchSettings.copy(sort = Sort.ByRelevanceDesc, types = List(LearningResourceType.Standard.toString)))
    results2.totalCount should be(3)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val Success(results) = multiSearchService.matchingQuery("bil", searchSettings.copy(sort = Sort.ByRelevanceDesc))
    val hits = results.results
    results.totalCount should be(3)
    hits.head.id should be(5)
    hits(1).id should be(1)
    hits.last.id should be(3)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(results) = multiSearchService.matchingQuery("bil", searchSettings.copy(sort = Sort.ByRelevanceDesc, withIdIn = List(3)))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val Success(results) = multiSearchService.matchingQuery("Pingvinen", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(2)
  }

  test("That search matches tags") {
    val Success(results) = multiSearchService.matchingQuery("and", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val Success(results) = multiSearchService.matchingQuery("supermann", searchSettings.copy(sort = Sort.ByTitleAsc))
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val Success(results) = multiSearchService.matchingQuery("supermann", searchSettings.copy(license = Some("copyrighted"), sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) = multiSearchService.matchingQuery("bilde + bil", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits1 = search1.results
    hits1.map(_.id) should equal (Seq(1, 3, 5))

    val Success(search2) = multiSearchService.matchingQuery("batmen + bil", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits2 = search2.results
    hits2.map(_.id) should equal (Seq(1))

    val Success(search3) = multiSearchService.matchingQuery("bil + bilde + -flaggermusmann", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits3 = search3.results
    hits3.map(_.id) should equal (Seq(3, 5))

    val Success(search4) = multiSearchService.matchingQuery("bil + -hulken", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits4 = search4.results
    hits4.map(_.id) should equal (Seq(1, 3))
  }

  test("search in content should be ranked lower than introduction and title") {
    val Success(search) = multiSearchService.matchingQuery("mareritt+ragnarok", searchSettings.copy(sort = Sort.ByRelevanceDesc))
    val hits = search.results
    hits.map(_.id) should equal (Seq(9, 8))
  }

  test("Search for all languages should return all articles in different languages") {
    val Success(search) = multiSearchService.all(searchSettings.copy(sort = Sort.ByTitleAsc, pageSize = 100, language = Language.AllLanguages))

    search.totalCount should equal(10)
  }

  test("Search for all languages should return all articles in correct language") {
    val Success(search) = multiSearchService.all(searchSettings.copy(pageSize = 100, language = Language.AllLanguages))
    val hits = search.results

    search.totalCount should equal(10)
    hits(0).id should equal(1)
    hits(1).id should equal(2)
    hits(2).id should equal(3)
    hits(3).id should equal(5)
    hits(4).id should equal(6)
    hits(5).id should equal(7)
    hits(6).id should equal(8)
    hits(7).id should equal(9)
    hits(8).id should equal(10)
    hits(9).id should equal(11)
    hits(8).title.language should equal("en")
    hits(9).title.language should equal("nb")
  }

  test("Search for all languages should return all languages if copyrighted") {
    val Success(search) = multiSearchService.all(searchSettings.copy(pageSize = 100, language = Language.AllLanguages, license = Some("copyrighted"), sort = Sort.ByTitleAsc))
    val hits = search.results

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val Success(searchEn) = multiSearchService.matchingQuery("Cats", searchSettings.copy(language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))
    val Success(searchNb) = multiSearchService.matchingQuery("Katter", searchSettings.copy(language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))

    searchEn.totalCount should equal(1)
    searchEn.results.head.id should equal(11)
    searchEn.results.head.title.title should equal("Cats")
    searchEn.results.head.title.language should equal("en")


    searchNb.totalCount should equal(1)
    searchNb.results.head.id should equal(11)
    searchNb.results.head.title.title should equal("Katter")
    searchNb.results.head.title.language should equal("nb")
  }

  test("metadescription is searchable") {
    val Success(search) = multiSearchService.matchingQuery("hurr dirr", searchSettings.copy(language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))

    search.totalCount should equal(1)
    search.results.head.id should equal(11)
    search.results.head.title.title should equal("Cats")
    search.results.head.title.language should equal("en")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val Success(search) = multiSearchService.all(searchSettings.copy(withIdIn = List(9, 10, 11), language = "en", fallback = true))

    search.totalCount should equal(3)
    search.results.head.id should equal(9)
    search.results.head.title.language should equal("nb")
    search.results(1).id should equal(10)
    search.results(1).title.language should equal("en")
    search.results(2).id should equal(11)
    search.results(2).title.language should equal("en")
  }

  test("That filtering for levels/filters works as expected") {
    val Success(search) = multiSearchService.all(searchSettings.copy(withIdIn = List(9, 10, 11), language = "en"))


  }

  def blockUntil(predicate: () => Boolean) = {
    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable => println("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting for predicate")
  }
}
