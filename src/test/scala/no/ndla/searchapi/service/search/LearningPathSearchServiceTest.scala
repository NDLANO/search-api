/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.embedded.LocalNode
import no.ndla.searchapi.integration.NdlaE4sClient
import no.ndla.searchapi.model.domain.{Author, Sort, Tag, Title}
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.searchapi.{SearchApiProperties, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.model.api.learningpath.LearningPathTags
import no.ndla.searchapi.model.taxonomy._
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.util.Success

class LearningPathSearchServiceTest extends UnitSuite with TestEnvironment {
  val esPort = 9205
  val localNodeSettings: Map[String, String] = LocalNode.requiredSettings(this.getClass.getName, s"/tmp/${this.getClass.getName}") + ("http.port" -> s"$esPort")
  val localNode = LocalNode(localNodeSettings)

  override val e4sClient = NdlaE4sClient(localNode.http(true))

  override val searchConverterService: SearchConverterService = new SearchConverterService
  override val learningPathIndexService = new LearningPathIndexService
  override val learningPathSearchService: LearningPathSearchService = new LearningPathSearchService

  override def beforeAll() = {
    learningPathIndexService.createIndexWithName(SearchApiProperties.SearchIndexes("learningpaths"))

    learningPathsToIndex.zipWithIndex.foreach{case (lp: LearningPath, index: Int) =>
          val resources = List(Resource(s"urn:resource:$index", lp.title.head.title, Some(s"urn:learningpath:${lp.id.get}"), s"/subject:1/topic:100/resource:$index"))
          val topics = List(Resource("urn:topic:100", "Topic1", Some("urn:learningpath:100"), "/subject:1/topic:100"))
          val topicResourceConnections = List(TopicResourceConnection("urn:topic:100", s"urn:resource:$index", "urn:topic-resource:abc123", true, 1))
          val subjects = List(Resource("urn:subject:1", "Subject1", None, "/subject:1"))
          val subjectTopicConnections = List(SubjectTopicConnection("urn:subject:1", "urn:topic:100", "urn:subject-topic:8180abc", true, 1))

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
      learningPathIndexService.indexDocument(lp, Some(generatedBundle))
    }

    blockUntil(() => learningPathIndexService.countDocuments == 5)
  }

  override def afterAll(): Unit = {
    localNode.stop(true)
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    learningPathSearchService.getStartAtAndNumResults(0, 1000) should equal((0, SearchApiProperties.MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 100
    val pageSize = 10
    val expectedStartAt = (page - 1) * pageSize
    learningPathSearchService.getStartAtAndNumResults(page, pageSize) should equal((expectedStartAt, pageSize))
  }

  test("That all learningpaths are returned ordered by title descending") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, None, "nb", Sort.ByTitleDesc, 1, 10, fallback = false)
    val hits = searchResult.results
    searchResult.totalCount should be(4)

    hits(0).id should be(UnrelatedId)
    hits(1).id should be(PenguinId)
    hits(2).id should be(DonaldId)
    hits(3).id should be(BatmanId)

  }

  test("That all learningpaths are returned ordered by title ascending") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, None, "nb", Sort.ByTitleAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits(0).id should be(BatmanId)
    hits(1).id should be(DonaldId)
    hits(2).id should be(PenguinId)
    hits(3).id should be(UnrelatedId)
  }

  test("That all learningpaths are returned ordered by id descending") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, None, "nb", Sort.ByIdDesc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits(0).id should be(UnrelatedId)
    hits(1).id should be(DonaldId)
    hits(2).id should be(BatmanId)
    hits(3).id should be(PenguinId)
  }

  test("That all learningpaths are returned ordered by id ascending") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, None, "all", Sort.ByIdAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(5)
    hits(0).id should be(PenguinId)
    hits(1).id should be(BatmanId)
    hits(2).id should be(DonaldId)
    hits(3).id should be(UnrelatedId)
    hits(4).id should be(EnglandoId)
  }

  test("That order by durationDesc orders search result by duration descending") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, None, "nb", Sort.ByDurationDesc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(UnrelatedId)
  }

  test("That order ByDurationAsc orders search result by duration ascending") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, None, "nb", Sort.ByDurationAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(PenguinId)
  }

  test("That order ByLastUpdatedDesc orders search result by last updated date descending") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, None, "nb", Sort.ByLastUpdatedDesc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(UnrelatedId)
    hits.last.id should be(PenguinId)
  }

  test("That order ByLastUpdatedAsc orders search result by last updated date ascending") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, None, "nb", Sort.ByLastUpdatedAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(PenguinId)
    hits.last.id should be(UnrelatedId)
  }

  test("That all filtered by id only returns learningpaths with the given ids") {
    val Success(searchResult) = learningPathSearchService.all(List(1, 2), None, "all", Sort.ByTitleAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(BatmanId)
    hits.last.id should be(PenguinId)
  }

  test("That searching only returns documents matching the query") {
    val Success(searchResult) = learningPathSearchService.matchingQuery("heltene", List.empty, None, "nb", Sort.ByTitleAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(searchResult) = learningPathSearchService.matchingQuery("morsom", List(3), None, "nb", Sort.ByTitleAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That searching only returns documents matching the query in the specified language") {
    val Success(searchResult) = learningPathSearchService.matchingQuery("guy", List.empty, None, "en", Sort.ByTitleAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That filtering on tag only returns documents where the tag is present") {
    val Success(searchResult) = learningPathSearchService.all(List.empty, Some("superhelt"), "nb", Sort.ByTitleAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(BatmanId)
    hits.last.id should be(PenguinId)
  }

  test("That filtering on tag combined with search only returns documents where the tag is present and the search matches the query") {
    val Success(searchResult) = learningPathSearchService.matchingQuery("heltene", List.empty, Some("kanfly"), "nb", Sort.ByTitleAsc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That searching and ordering by relevance is returning Donald before Batman when searching for tough weirdos") {
    val Success(searchResult) = learningPathSearchService.matchingQuery("tøff rar", List.empty, None, "nb", Sort.ByRelevanceDesc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test("That searching and ordering by relevance is returning Donald before Batman and the penguin when searching for duck, bat and bird") {
    val Success(searchResult) = learningPathSearchService.matchingQuery("and flaggermus fugl", List.empty, None, "nb", Sort.ByRelevanceDesc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(3)
    hits.toList(0).id should be(DonaldId)
    hits.toList(1).id should be(BatmanId)
    hits.toList(2).id should be(PenguinId)
  }

  test("That searching and ordering by relevance is not returning Penguin when searching for duck, bat and bird, but filtering on kanfly") {
    val Success(searchResult) = learningPathSearchService.matchingQuery("and flaggermus fugl", List.empty, Some("kanfly"), "nb", Sort.ByRelevanceDesc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test("That a search for flaggremsu returns Donald but not Batman if it is misspelled") {
    val Success(searchResult) = learningPathSearchService.matchingQuery("and flaggremsu", List.empty, None, "nb", Sort.ByRelevanceDesc, 1, 10, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That searching with logical operators works") {
    val Success(searchResult1) = learningPathSearchService.matchingQuery("kjeltring + batman", List.empty, None, "nb", Sort.ByRelevanceAsc, 1, 10, fallback = false)
    searchResult1.totalCount should be(0)

    val Success(searchResult2) = learningPathSearchService.matchingQuery("tøff + morsom + -and", List.empty, None, "nb", Sort.ByRelevanceAsc, 1, 10, fallback = false)
    val hits2 = searchResult2.results

    searchResult2.totalCount should be(1)
    hits2.head.id should be(BatmanId)

    val Success(searchResult3) = learningPathSearchService.matchingQuery("tøff | morsom | kjeltring", List.empty, None, "nb", Sort.ByRelevanceAsc, 1, 10, fallback = false)
    val hits3 = searchResult3.results

    searchResult3.totalCount should be(3)
    hits3.head.id should be(PenguinId)
    hits3(1).id should be(DonaldId)
    hits3.last.id should be(BatmanId)
  }

  test("That searching for multiple languages returns result in matched language") {
    val Success(searchEn) = learningPathSearchService.matchingQuery("Unrelated", List.empty, None, "all", Sort.ByTitleAsc, 1, 10, fallback = false)
    val Success(searchNb) = learningPathSearchService.matchingQuery("Urelatert", List.empty, None, "all", Sort.ByTitleAsc, 1, 10, fallback = false)

    searchEn.totalCount should be(1)
    searchEn.results.head.id should be(UnrelatedId)
    searchEn.results.head.title.language should be("en")
    searchEn.results.head.title.title should be("Unrelated")
    searchEn.results.head.description.description should be("This is unrelated")
    searchEn.results.head.description.language should be("en")

    searchNb.totalCount should be(1)
    searchNb.results.head.id should be(UnrelatedId)
    searchNb.results.head.title.language should be("nb")
    searchNb.results.head.title.title should be("Urelatert")
    searchNb.results.head.description.description should be("Dette er en urelatert")
    searchNb.results.head.description.language should be("nb")
  }

  test("That searching for all languages returns multiple languages") {
    val Success(search) = learningPathSearchService.all(List.empty, None, "all", Sort.ByTitleAsc, 1, 10, fallback = false)

    search.totalCount should be(5)
    search.results(0).id should be(BatmanId)
    search.results(1).id should be(DonaldId)
    search.results(2).id should be(EnglandoId)
    search.results(2).title.language should be("en")
    search.results(3).id should be(PenguinId)
    search.results(4).id should be(UnrelatedId)
    search.results(4).title.language should be("nb")
  }

  test("that supportedLanguages are sorted correctly") {
    val Success(search) = learningPathSearchService.matchingQuery("Batman", List.empty, None, "all", Sort.ByTitleAsc, 1, 10, fallback = false)
    search.results.head.supportedLanguages should be(Seq("nb", "en"))
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
