/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.nio.file.{Files, Path}

import com.sksamuel.elastic4s.embedded.LocalNode
import no.ndla.searchapi.SearchApiProperties.DefaultPageSize
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.integration.NdlaE4sClient
import no.ndla.searchapi.model.api.MetaImage
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.learningpath.LearningPath
import no.ndla.searchapi.model.domain.{Language, Sort}
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.{SearchApiProperties, TestData, TestEnvironment, UnitSuite}

import scala.util.Success

class MultiSearchServiceTest extends UnitSuite with TestEnvironment {
  val tmpDir: Path = Files.createTempDirectory(this.getClass.getName)
  val localNodeSettings: Map[String, String] = LocalNode.requiredSettings(this.getClass.getName, tmpDir.toString)
  val localNode = LocalNode(localNodeSettings)

  override val e4sClient = NdlaE4sClient(localNode.client(true))

  override val articleIndexService = new ArticleIndexService
  override val draftIndexService = new DraftIndexService
  override val learningPathIndexService = new LearningPathIndexService
  override val multiSearchService = new MultiSearchService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  override def beforeAll: Unit = {
    articleIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.Articles))
    draftIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.Drafts))
    learningPathIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.LearningPaths))

    val indexedArticles =
      articlesToIndex.map(article => articleIndexService.indexDocument(article, taxonomyTestBundle))

    val indexedDrafts = draftsToIndex.map(draft => draftIndexService.indexDocument(draft, taxonomyTestBundle))

    val indexedLearningPaths =
      learningPathsToIndex.map(lp => learningPathIndexService.indexDocument(lp, taxonomyTestBundle))

    blockUntil(() => {
      articleIndexService.countDocuments == articlesToIndex.size &&
      learningPathIndexService.countDocuments == learningPathsToIndex.size
    })
  }

  def hasTaxonomy(lp: LearningPath): Boolean =
    (taxonomyTestBundle.resources.map(_.contentUri.get) ++ taxonomyTestBundle.topics.map(_.contentUri.get))
      .contains(s"urn:learningpath:${lp.id.get}")

  def hasTaxonomy(ar: Article): Boolean =
    (taxonomyTestBundle.resources.map(_.contentUri.get) ++ taxonomyTestBundle.topics.map(_.contentUri.get))
      .contains(s"urn:article:${ar.id.get}")

  private def expectedAllPublicArticles(language: String) = {
    val x = if (language == "all") { TestData.articlesToIndex } else {
      TestData.articlesToIndex.filter(_.title.map(_.language).contains(language))
    }
    x.filter(_.copyright.license != "copyrighted")
      .filter(hasTaxonomy)
  }

  private def expectedAllPublicLearningPaths(language: String) = {
    val x = if (language == "all") { TestData.learningPathsToIndex } else {
      TestData.learningPathsToIndex.filter(_.title.map(_.language).contains(language))
    }
    x.filter(_.copyright.license != "copyrighted")
      .filter(hasTaxonomy)
  }

  private def idsForLang(language: String) =
    expectedAllPublicArticles(language).map(_.id.get) ++
      expectedAllPublicLearningPaths(language).map(_.id.get)

  private def titlesForLang(language: String) = {
    expectedAllPublicArticles(language).map(_.title.find(_.language == language || language == "all").get.title) ++
      expectedAllPublicLearningPaths(language).map(_.title.find(_.language == language || language == "all").get.title)
  }

  override def afterAll(): Unit = {
    localNode.stop(true)
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    multiSearchService.getStartAtAndNumResults(0, 1000) should equal((0, SearchApiProperties.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByIdAsc))

    val hits = results.results
    results.totalCount should be(idsForLang("nb").size)
    hits.map(_.id) should be(idsForLang("nb").sorted)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByIdDesc))

    val hits = results.results
    results.totalCount should be(idsForLang("nb").size)
    hits.map(_.id) should be(idsForLang("nb").sorted.reverse)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(titlesForLang("nb").size)
    hits.map(_.title.title) should be(titlesForLang("nb").sorted)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByTitleDesc))
    val hits = results.results
    results.totalCount should be(titlesForLang("nb").size)
    hits.map(_.title.title) should be(titlesForLang("nb").sorted.reverse)
  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByLastUpdatedDesc))
    val hits = results.results
    results.totalCount should be(idsForLang("nb").size)
    hits.head.id should be(3)
    hits.last.id should be(5)
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val Success(results) = multiSearchService.all(searchSettings.copy(sort = Sort.ByLastUpdatedAsc))
    val hits = results.results
    results.totalCount should be(idsForLang("nb").size)
    hits.head.id should be(5)
    hits(1).id should be(1)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) = multiSearchService.all(searchSettings.copy(page = 1, pageSize = 2, sort = Sort.ByTitleAsc))
    val Success(page2) = multiSearchService.all(searchSettings.copy(page = 2, pageSize = 2, sort = Sort.ByTitleAsc))
    val hits1 = page1.results
    val hits2 = page2.results
    page1.totalCount should be(idsForLang("nb").size)
    page1.page should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(2)
    page2.totalCount should be(idsForLang("nb").size)
    page2.page should be(2)
    hits2.size should be(2)
    hits2.head.id should be(1)
    hits2.last.id should be(3)
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
    val Success(results) =
      multiSearchService.matchingQuery("bil", searchSettings.copy(sort = Sort.ByRelevanceDesc, withIdIn = List(3)))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val Success(results) = multiSearchService.matchingQuery("Pingvinen", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(1)
    hits.head.contexts.head.learningResourceType should be("learningpath")
    hits(1).id should be(2)
  }

  test("That search matches tags") {
    val Success(results) = multiSearchService.matchingQuery("and", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(3)
    hits(1).id should be(3)
    hits(1).contexts.head.learningResourceType should be("learningpath")
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val Success(results) = multiSearchService.matchingQuery("supermann", searchSettings.copy(sort = Sort.ByTitleAsc))
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val Success(results) =
      multiSearchService.matchingQuery("supermann",
                                       searchSettings.copy(license = Some("copyrighted"), sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) = multiSearchService.matchingQuery("bilde + bil", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits1 = search1.results
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val Success(search2) = multiSearchService.matchingQuery("batmen + bil", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits2 = search2.results
    hits2.map(_.id) should equal(Seq(1))

    val Success(search3) =
      multiSearchService.matchingQuery("bil + bilde + -flaggermusmann", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits3 = search3.results
    hits3.map(_.id) should equal(Seq(3, 5))

    val Success(search4) =
      multiSearchService.matchingQuery("bil + -hulken", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits4 = search4.results
    hits4.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than introduction and title") {
    val Success(search) =
      multiSearchService.matchingQuery("mareritt+ragnarok", searchSettings.copy(sort = Sort.ByRelevanceDesc))
    val hits = search.results
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search for all languages should return all articles in different languages") {
    val Success(search) = multiSearchService.all(
      searchSettings.copy(language = Language.AllLanguages, pageSize = 100, sort = Sort.ByTitleAsc))

    search.totalCount should equal(titlesForLang("all").size)
  }

  test("Search for all languages should return all articles in correct language") {
    val Success(search) = multiSearchService.all(searchSettings.copy(language = Language.AllLanguages, pageSize = 100))
    val hits = search.results

    val exp = titlesForLang("all")

    search.totalCount should equal(exp.size)
    hits.head.id should be(1)
    hits(1).id should be(1)
    hits(2).id should be(2)
    hits(3).id should be(2)
    hits(4).id should be(3)
    hits(5).id should be(3)
    hits(6).id should be(4)
    hits(7).id should be(5)
    hits(8).id should be(5)
    hits(8).title.language should be("en")
    hits(9).id should be(6)
    hits(10).id should be(7)
    hits(11).id should be(8)
    hits(12).id should be(9)
    hits(13).id should be(10)
    hits(13).title.language should be("en")
    hits(14).id should be(11)
    hits(14).title.language should be("nb")
  }

  test("Search for all languages should return all languages if copyrighted") {
    val Success(search) = multiSearchService.all(
      searchSettings
        .copy(language = Language.AllLanguages, license = Some("copyrighted"), pageSize = 100, sort = Sort.ByTitleAsc))
    val hits = search.results

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val Success(searchEn) = multiSearchService.matchingQuery(
      "Cats",
      searchSettings.copy(language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))
    val Success(searchNb) = multiSearchService.matchingQuery(
      "Katter",
      searchSettings.copy(language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))

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
    val Success(search) = multiSearchService.matchingQuery(
      "hurr dirr",
      searchSettings.copy(language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))

    search.totalCount should equal(1)
    search.results.head.id should equal(11)
    search.results.head.title.title should equal("Cats")
    search.results.head.title.language should equal("en")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val Success(search) =
      multiSearchService.all(searchSettings.copy(fallback = true, language = "en", withIdIn = List(9, 10, 11)))

    search.totalCount should equal(3)
    search.results.head.id should equal(9)
    search.results.head.title.language should equal("nb")
    search.results(1).id should equal(10)
    search.results(1).title.language should equal("en")
    search.results(2).id should equal(11)
    search.results(2).title.language should equal("en")
  }

  test("That filtering for levels/filters on resources works as expected") {
    val Success(search) =
      multiSearchService.all(searchSettings.copy(language = "all", taxonomyFilters = List("YF-VG1")))
    search.totalCount should be(2)
    search.results.map(_.id) should be(Seq(6, 7))

    val Success(search2) = multiSearchService.all(searchSettings.copy(language = "all", taxonomyFilters = List("VG2")))
    search2.totalCount should be(5)
    search2.results.map(_.id) should be(Seq(1, 3, 5, 6, 12))
  }

  test("That filtering for multiple levels/filters returns resources from all filters") {
    val Success(search3) =
      multiSearchService.all(searchSettings.copy(language = "nb", taxonomyFilters = List("YF-VG1", "VG1")))
    search3.totalCount should be(5)
    search3.results.map(_.id) should be(Seq(1, 5, 6, 7, 12))
  }

  test("That filtering for levels/filters works with spaces as well") {
    val Success(search) = multiSearchService.all(searchSettings.copy(language = "nb", taxonomyFilters = List("Tysk 2")))
    search.totalCount should be(1)
    search.results.map(_.id) should be(Seq(3))
  }

  test("That filtering for subjects works as expected") {
    val Success(search) =
      multiSearchService.all(searchSettings.copy(subjects = List("urn:subject:2"), language = "all"))
    search.totalCount should be(7)
    search.results.map(_.id) should be(Seq(1, 5, 5, 6, 7, 11, 12))
  }

  test("That filtering for subjects returns all resources with any of listed subjects") {
    val Success(search) = multiSearchService.all(searchSettings.copy(subjects = List("urn:subject:2", "urn:subject:1")))
    search.totalCount should be(14)
    search.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 11, 12))
  }

  test("That filtering for resource-types works as expected") {
    val Success(search) =
      multiSearchService.all(searchSettings.copy(resourceTypes = List("urn:resourcetype:academicArticle")))
    search.totalCount should be(2)
    search.results.map(_.id) should be(Seq(2, 5))

    val Success(search2) =
      multiSearchService.all(searchSettings.copy(resourceTypes = List("urn:resourcetype:subjectMaterial")))
    search2.totalCount should be(7)
    search2.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))

    val Success(search3) =
      multiSearchService.all(searchSettings.copy(resourceTypes = List("urn:resourcetype:learningpath")))
    search3.totalCount should be(4)
    search3.results.map(_.id) should be(Seq(1, 2, 3, 4))
  }

  test("That filtering for multiple resource-types returns resources from both") {
    val Success(search) = multiSearchService.all(
      searchSettings.copy(resourceTypes = List("urn:resourcetype:subjectMaterial", "urn:resourcetype:reviewResource")))
    search.totalCount should be(7)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))
  }

  test("That filtering on learning-resource-type works") {
    val Success(search) = multiSearchService.all(
      searchSettings.copy(learningResourceTypes = List(LearningResourceType.Article), language = "all"))
    val Success(search2) = multiSearchService.all(
      searchSettings.copy(learningResourceTypes = List(LearningResourceType.TopicArticle), language = "all"))

    search.totalCount should be(7)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))

    search2.totalCount should be(4)
    search2.results.map(_.id) should be(Seq(8, 9, 10, 11))
  }

  test("That filtering on multiple context-types returns every type") {
    val Success(search) = multiSearchService.all(
      searchSettings.copy(learningResourceTypes = List(LearningResourceType.Article, LearningResourceType.TopicArticle),
                          language = "all"))

    search.totalCount should be(11)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12))
  }

  test("That filtering on learningpath learningresourcetype returns learningpaths") {
    val Success(search) = multiSearchService.all(
      searchSettings.copy(learningResourceTypes = List(LearningResourceType.LearningPath), language = "all"))

    search.totalCount should be(5)
    search.results.map(_.id) should be(Seq(1, 2, 3, 4, 5))
    search.results.map(_.contexts.head.learningResourceType) should be(
      Seq.fill(5) { LearningResourceType.LearningPath.toString }
    )
  }

  test("That filtering on supportedLanguages works") {
    val Success(search) = multiSearchService.all(searchSettings.copy(supportedLanguages = List("en"), language = "all"))
    search.totalCount should be(6)
    search.results.map(_.id) should be(Seq(2, 3, 4, 5, 10, 11))

    val Success(search2) =
      multiSearchService.all(searchSettings.copy(supportedLanguages = List("en", "nb"), language = "all"))
    search2.totalCount should be(16)
    search2.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9, 10, 11, 12))

    val Success(search3) =
      multiSearchService.all(searchSettings.copy(supportedLanguages = List("nb"), language = "all"))
    search3.totalCount should be(14)
    search3.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 11, 12))
  }

  test("That filtering on supportedLanguages should still prioritize the selected language") {
    val Success(search) = multiSearchService.all(searchSettings.copy(supportedLanguages = List("en"), language = "nb"))

    search.totalCount should be(4)
    search.results.map(_.id) should be(Seq(2, 3, 4, 11))
    search.results.map(_.title.language) should be(Seq("nb", "nb", "nb", "nb"))
  }

  test("That meta image are returned when searching") {
    val Success(search) = multiSearchService.all(searchSettings.copy(language = "en", withIdIn = List(10)))

    search.totalCount should be(1)
    search.results.head.id should be(10)
    search.results.head.metaImage should be(
      Some(MetaImage("http://api-gateway.ndla-local/image-api/raw/id/442", "alt", "en")))
  }

  test("That learningpaths without taxonomy are not returned") {
    val Success(search) = multiSearchService.all(searchSettings.copy(language = "en", withIdIn = List(6)))

    search.totalCount should be(0)
  }

  test("That searching for authors works as expected") {
    val Success(search1) =
      multiSearchService.matchingQuery("Kjekspolitiet", searchSettings.copy(language = Language.AllLanguages))
    search1.totalCount should be(1)
    search1.results.map(_.id) should be(Seq(1))

    val Success(search2) =
      multiSearchService.matchingQuery("Svims", searchSettings.copy(language = Language.AllLanguages))
    search2.totalCount should be(2)
    search2.results.map(_.id) should be(Seq(2, 5))
  }

  test("That filtering by relevance id makes sense (with and without subject/filter)") {
    val Success(search1) = multiSearchService.all(
      searchSettings.copy(language = Language.AllLanguages, relevanceIds = List("urn:relevance:core")))
    search1.results.map(_.id) should be(Seq(1, 5, 6, 7, 12))

    val Success(search2) = multiSearchService.all(
      searchSettings.copy(language = Language.AllLanguages, relevanceIds = List("urn:relevance:supplementary")))
    search2.results.map(_.id) should be(Seq(1, 3, 12))

    val Success(search3) = multiSearchService.all(
      searchSettings.copy(language = Language.AllLanguages,
                          relevanceIds = List("urn:relevance:supplementary", "urn:relevance:core")))
    search3.results.map(_.id) should be(Seq(1, 3, 5, 6, 7, 12))
  }

  test("That filtering by relevance and subject only returns for relevances in filtered subjects") {
    val Success(search1) = multiSearchService.all(
      searchSettings.copy(language = Language.AllLanguages,
                          relevanceIds = List("urn:relevance:core"),
                          subjects = List("urn:subject:1")))

    search1.results.map(_.id) should be(Seq(1, 5))
  }

  def blockUntil(predicate: () => Boolean): Unit = {
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
