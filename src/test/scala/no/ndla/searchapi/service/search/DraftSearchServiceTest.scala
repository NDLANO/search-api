/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */


package no.ndla.searchapi.service.search

import java.nio.file.{Files, Path}

import com.sksamuel.elastic4s.embedded.LocalNode
import no.ndla.searchapi.integration.NdlaE4sClient
import no.ndla.searchapi.{SearchApiProperties, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.model.domain.{Language, Sort}
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.draft.Draft
import no.ndla.searchapi.model.taxonomy.{Bundle, Resource, SubjectTopicConnection, TopicResourceConnection}
import org.joda.time.DateTime

import scala.util.Success

class DraftSearchServiceTest extends UnitSuite with TestEnvironment {
  val tmpDir: Path = Files.createTempDirectory(this.getClass.getName)
  val localNodeSettings: Map[String, String] = LocalNode.requiredSettings(this.getClass.getName, tmpDir.toString)
  val localNode = LocalNode(localNodeSettings)

  override val e4sClient = NdlaE4sClient(localNode.http(true))

  override val draftIndexService = new DraftIndexService
  override val draftSearchService = new DraftSearchService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  override def beforeAll: Unit = {
    draftIndexService.createIndexWithName(SearchApiProperties.SearchIndexes("drafts"))

    draftsToIndex.zipWithIndex.map{case (draft: Draft, index: Int) =>
      val (
        resources,
        topics,
        topicResourceConnections,
        subjects,
        subjectTopicConnections
        ) = draft.articleType match {
        case LearningResourceType.Article =>
          val resources = List(Resource(s"urn:resource:$index", draft.title.head.title, Some(s"urn:article:${draft.id.get}"), s"/subject:1/topic:100/resource:$index"))
          val topics = List(Resource("urn:topic:100", "Topic1", Some("urn:article:100"), "/subject:1/topic:100"))
          val topicResourceConnections = List(TopicResourceConnection("urn:topic:100", s"urn:resource:$index", "urn:topic-resource:abc123", true, 1))
          val subjects = List(Resource("urn:subject:1", "Subject1", None, "/subject:1"))
          val subjectTopicConnections = List(SubjectTopicConnection("urn:subject:1", "urn:topic:100", "urn:subject-topic:8180abc", true, 1))

          (resources, topics, topicResourceConnections, subjects, subjectTopicConnections)
        case LearningResourceType.TopicArticle =>
          val resources = List()
          val topicResourceConnections = List()
          val topics = List(Resource(s"urn:topic:$index", draft.title.head.title, Some(s"urn:article:${draft.id.get}"), s"/subject:1/topic:$index"))
          val subjects = List(Resource("urn:subject:1", "Subject1", None, "/subject:1"))
          val subjectTopicConnections = List(SubjectTopicConnection("urn:subject:1", s"urn:topic:$index", "urn:subject-topic:8180abc", true, 1))

          (resources, topics, topicResourceConnections, subjects, subjectTopicConnections)
        case _ =>
          (List.empty, List.empty, List.empty, List.empty, List.empty)
      }

      val generatedBundle = Bundle(
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
        topics = topics
      )

      draftIndexService.indexDocument(draft, Some(generatedBundle))

    }

    blockUntil(() => draftIndexService.countDocuments == 11)
  }

  override def afterAll(): Unit = {
    localNode.stop(true)
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    draftSearchService.getStartAtAndNumResults(0, 1000) should equal((0, SearchApiProperties.MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * SearchApiProperties.DefaultPageSize
    draftSearchService.getStartAtAndNumResults(page, SearchApiProperties.DefaultPageSize) should equal((expectedStartAt, SearchApiProperties.DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * SearchApiProperties.DefaultPageSize
    draftSearchService.getStartAtAndNumResults(page, SearchApiProperties.DefaultPageSize) should equal((expectedStartAt, SearchApiProperties.DefaultPageSize))
  }

  test("all should return only articles of a given type if a type filter is specified") {
    val Success(results) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq(LearningResourceType.TopicArticle.toString), fallback = false)
    results.totalCount should be(3)
    results.results.map(_.id) should be(Seq(8, 9, 11))

    val Success(results2) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, LearningResourceType.all, fallback = false)
    results2.totalCount should be(9)
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(1)
    hits(1).id should be(2)
    hits(2).id should be(3)
    hits(3).id should be(5)
    hits(4).id should be(6)
    hits(5).id should be(7)
    hits(6).id should be(8)
    hits(7).id should be(9)
    hits.last.id should be(11)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByIdDesc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be (11)
    hits.last.id should be (1)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(8)
    hits(1).id should be(9)
    hits(2).id should be(1)
    hits(3).id should be(3)
    hits(4).id should be(5)
    hits(5).id should be(11)
    hits(6).id should be(6)
    hits(7).id should be(2)
    hits.last.id should be(7)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByTitleDesc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(7)
    hits(1).id should be(2)
    hits(2).id should be(6)
    hits(3).id should be(11)
    hits(4).id should be(5)
    hits(5).id should be(3)
    hits(6).id should be(1)
    hits(7).id should be(9)
    hits.last.id should be(8)
  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val Success(results) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByLastUpdatedDesc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(9)
    hits.head.id should be(3)
    hits.last.id should be(5)
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val Success(results) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 10, Sort.ByLastUpdatedAsc, Seq.empty, fallback = false)
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

  test("That all filtering on license only returns documents with given license") {
    val Success(results) = draftSearchService.all(List(), Language.DefaultLanguage, Some("publicdomain"), 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(8)
    hits.head.id should be(8)
    hits(1).id should be(9)
    hits(2).id should be(3)
    hits(3).id should be(5)
    hits(4).id should be(11)
    hits(5).id should be(6)
    hits(6).id should be(2)
    hits.last.id should be(7)
  }

  test("That all filtered by id only returns documents with the given ids") {
    val Success(results) = draftSearchService.all(List(1, 3), Language.DefaultLanguage, None, 1, 10, Sort.ByIdAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(1)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) = draftSearchService.all(List(), Language.DefaultLanguage, None, 1, 2, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits1 = page1.results
    page1.totalCount should be(9)
    page1.page should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(9)

    val Success(page2) = draftSearchService.all(List(), Language.DefaultLanguage, None, 2, 2, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits2 = page2.results
    page2.totalCount should be(9)
    page2.page should be(2)
    hits2.size should be(2)
    hits2.head.id should be(1)
    hits2.last.id should be(3)
  }

  test("mathcingQuery should filter results based on an article type filter") {
    val Success(results) = draftSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq(LearningResourceType.TopicArticle.toString), fallback = false)
    results.totalCount should be(0)

    val Success(results2) = draftSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq(LearningResourceType.Article.toString), fallback = false)
    results2.totalCount should be(3)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val Success(results) = draftSearchService.matchingQuery("bil", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(3)
    hits.head.id should be(5)
    hits(1).id should be(1)
    hits.last.id should be(3)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(results) = draftSearchService.matchingQuery("bil", List(3), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
  }

  test("That search matches title") {
    val Success(results) = draftSearchService.matchingQuery("Pingvinen", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(2)
  }

  test("That search matches tags") {
    val Success(results) = draftSearchService.matchingQuery("and", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val Success(results) = draftSearchService.matchingQuery("supermann", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val Success(results) = draftSearchService.matchingQuery("supermann", List(), "nb", Some("copyrighted"), 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) = draftSearchService.matchingQuery("bilde + bil", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits1 = search1.results
    hits1.map(_.id) should equal (Seq(1, 3, 5))

    val Success(search2) = draftSearchService.matchingQuery("batmen + bil", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits2 = search2.results
    hits2.map(_.id) should equal (Seq(1))

    val Success(search3) = draftSearchService.matchingQuery("bil + bilde - flaggermusmann", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits3 = search3.results
    hits3.map(_.id) should equal (Seq(1, 3, 5))

    val Success(search4) = draftSearchService.matchingQuery("bil - hulken", List(), "nb", None, 1, 10, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits4 = search4.results
    hits4.map(_.id) should equal (Seq(1, 3, 5))
  }

  test("search in content should be ranked lower than introduction and title") {
    val Success(search) = draftSearchService.matchingQuery("mareritt + ragnarok", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty, fallback = false)
    val hits = search.results
    hits.map(_.id) should equal (Seq(9, 8))
  }

  test("searching for notes should return relevant results") {
    val Success(search) = draftSearchService.matchingQuery("kakemonster", List(), "nb", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty, fallback = false)
    search.totalCount should be (1)
    search.results.head.id should be (5)
  }

  test("Search for all languages should return all articles in correct language") {
    val Success(search) = draftSearchService.all(List(), Language.AllLanguages, None, 1, 100, Sort.ByIdAsc, Seq.empty, fallback = false)
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
    val Success(search) = draftSearchService.all(List(), Language.AllLanguages, Some("copyrighted"), 1, 100, Sort.ByTitleAsc, Seq.empty, fallback = false)
    val hits = search.results

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val Success(searchEn) = draftSearchService.matchingQuery("Big", List(), "all", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty, fallback = false)
    val Success(searchNb) = draftSearchService.matchingQuery("Store", List(), "all", None, 1, 10, Sort.ByRelevanceDesc, Seq.empty, fallback = false)

    searchEn.totalCount should equal(1)
    searchEn.results.head.id should equal(11)
    searchEn.results.head.title.title should equal("Cats")
    searchEn.results.head.title.language should equal("en")


    searchNb.totalCount should equal(1)
    searchNb.results.head.id should equal(11)
    searchNb.results.head.title.title should equal("Katter")
    searchNb.results.head.title.language should equal("nb")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val Success(search) = draftSearchService.all(List(9, 10, 11), "en", None, 1, 10, Sort.ByIdAsc, Seq.empty, fallback = true)

    search.totalCount should equal(3)
    search.results.head.id should equal(9)
    search.results.head.title.language should equal("nb")
    search.results(1).id should equal(10)
    search.results(1).title.language should equal("en")
    search.results(2).id should equal(11)
    search.results(2).title.language should equal("en")
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
