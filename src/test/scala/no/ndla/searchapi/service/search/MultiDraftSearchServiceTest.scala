/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.nio.file.{Files, Path}

import no.ndla.searchapi.SearchApiProperties.DefaultPageSize
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.integration.{Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.searchapi.model.api.MetaImage
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.{Language, Sort}
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.{IntegrationSuite, SearchApiProperties, TestEnvironment, UnitSuite}
import org.scalatest.Outcome

import scala.util.Success

class MultiDraftSearchServiceTest extends IntegrationSuite with TestEnvironment {
  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val articleIndexService = new ArticleIndexService
  override val draftIndexService = new DraftIndexService
  override val learningPathIndexService = new LearningPathIndexService
  override val multiDraftSearchService = new MultiDraftSearchService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  override def beforeAll: Unit = if (elasticSearchContainer.isSuccess) {
    articleIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.Articles))
    draftIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.Drafts))
    learningPathIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.LearningPaths))

    val indexedArticles =
      articlesToIndex.map(article => articleIndexService.indexDocument(article, taxonomyTestBundle, emptyGrepBundle))

    val indexedDrafts =
      draftsToIndex.map(draft => draftIndexService.indexDocument(draft, taxonomyTestBundle, emptyGrepBundle))

    val indexedLearningPaths =
      learningPathsToIndex.map(lp => learningPathIndexService.indexDocument(lp, taxonomyTestBundle, emptyGrepBundle))

    blockUntil(() => {
      articleIndexService.countDocuments == articlesToIndex.size &&
      learningPathIndexService.countDocuments == learningPathsToIndex.size
    })
  }

  private def expectedAllPublicDrafts(language: String) = {
    val x = if (language == "all") { draftsToIndex } else {
      draftsToIndex.filter(_.title.map(_.language).contains(language))
    }
    x.filter(!_.copyright.flatMap(_.license).contains("copyrighted"))
      .filterNot(_.status.current == ArticleStatus.ARCHIVED)
  }

  private def expectedAllPublicLearningPaths(language: String) = {
    val x = if (language == "all") { learningPathsToIndex } else {
      learningPathsToIndex.filter(_.title.map(_.language).contains(language))
    }
    x.filter(_.copyright.license != "copyrighted")
  }

  private def idsForLang(language: String) =
    expectedAllPublicDrafts(language).map(_.id.get) ++
      expectedAllPublicLearningPaths(language).map(_.id.get)

  private def titlesForLang(language: String) = {
    expectedAllPublicDrafts(language).map(_.title.find(_.language == language || language == "all").get.title) ++
      expectedAllPublicLearningPaths(language).map(_.title.find(_.language == language || language == "all").get.title)
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    multiDraftSearchService.getStartAtAndNumResults(0, 10001) should equal((0, SearchApiProperties.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiDraftSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiDraftSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) = multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByIdAsc))
    val expected = idsForLang("nb").sorted
    results.totalCount should be(expected.size)
    results.results.map(_.id) should be(expected)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) = multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByIdDesc))
    val expected = idsForLang("nb").sorted.reverse
    results.totalCount should be(expected.size)
    results.results.map(_.id) should be(expected)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) = multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByTitleAsc))
    val expected = titlesForLang("nb").sorted
    results.totalCount should be(expected.size)
    results.results.map(_.title.title) should be(expected)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) = multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByTitleDesc))
    val expected = titlesForLang("nb").sorted.reverse
    results.totalCount should be(expected.size)
    results.results.map(_.title.title) should be(expected)
  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByLastUpdatedDesc))
    val expected = idsForLang("nb")
    results.totalCount should be(expected.size)
    results.results.head.id should be(3)
    results.results.last.id should be(5)
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByLastUpdatedAsc))
    val expected = idsForLang("nb")
    results.totalCount should be(expected.size)
    results.results.head.id should be(5)
    results.results(1).id should be(1)
    results.results.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(page = 1, pageSize = 2, sort = Sort.ByIdAsc))
    val Success(page2) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(page = 2, pageSize = 2, sort = Sort.ByIdAsc))
    val expected = idsForLang("nb")
    val hits1 = page1.results
    val hits2 = page2.results
    page1.totalCount should be(expected.size)
    page1.page.get should be(1)
    hits1.size should be(2)
    hits1.head.id should be(1)
    hits1.last.id should be(1)
    page2.totalCount should be(expected.size)
    page2.page.get should be(2)
    hits2.size should be(2)
    hits2.head.id should be(2)
    hits2.last.id should be(2)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("bil"), sort = Sort.ByRelevanceDesc))
    val hits = results.results
    results.totalCount should be(3)
    hits.head.id should be(1)
    hits(1).id should be(5)
    hits.last.id should be(3)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("bil"), sort = Sort.ByRelevanceDesc, withIdIn = List(3)))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("Pingvinen"), sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(1)
    hits.head.contexts.head.learningResourceType should be("learningpath")
    hits(1).id should be(2)
  }

  test("That search matches updatedBy") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(userFilter = List("ndalId54321"), sort = Sort.ByIdAsc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(1)
    hits.head.contexts.head.learningResourceType should be("standard")
    hits(1).id should be(2)
  }

  test("That search matches tags") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(query = Some("and"), sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(3)
    hits(1).id should be(3)
    hits(1).contexts.head.learningResourceType should be("learningpath")
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("supermann"), sort = Sort.ByTitleAsc))
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("supermann"), license = Some("copyrighted"), sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("bilde + bil"), sort = Sort.ByTitleAsc))
    val hits1 = search1.results
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val Success(search2) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("batmen + bil"), sort = Sort.ByTitleAsc))
    val hits2 = search2.results
    hits2.map(_.id) should equal(Seq(1))

    val Success(search3) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some("bil + bilde + -flaggermusmann"), sort = Sort.ByTitleAsc))
    val hits3 = search3.results
    hits3.map(_.id) should equal(Seq(3, 5))

    val Success(search4) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("bil + -hulken"), sort = Sort.ByTitleAsc))
    val hits4 = search4.results
    hits4.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than introduction and title") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("mareritt+ragnarok"), sort = Sort.ByRelevanceDesc))
    val hits = search.results
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search for all languages should return all articles in different languages") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = Language.AllLanguages, pageSize = 100, sort = Sort.ByTitleAsc))

    search.totalCount should equal(titlesForLang("all").size)
  }

  test("Search for all languages should return all articles in correct language") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(language = Language.AllLanguages, pageSize = 100))
    val hits = search.results

    search.totalCount should equal(idsForLang("all").size)
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
    hits(10).id should be(6)
    hits(11).id should be(7)
    hits(12).id should be(8)
    hits(13).id should be(9)
    hits(14).id should be(10)
    hits(14).title.language should be("en")
    hits(15).id should be(11)
    hits(15).title.language should be("nb")
  }

  test("Search for all languages should return all languages if copyrighted") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings
        .copy(language = Language.AllLanguages, license = Some("copyrighted"), pageSize = 100, sort = Sort.ByTitleAsc))
    val hits = search.results

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val Success(searchEn) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings
        .copy(query = Some("Cats"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))
    val Success(searchNb) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings
        .copy(query = Some("Katter"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))

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
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings
        .copy(query = Some("hurr dirr"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))

    search.totalCount should equal(1)
    search.results.head.id should equal(11)
    search.results.head.title.title should equal("Cats")
    search.results.head.title.language should equal("en")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(fallback = true, language = "en", withIdIn = List(9, 10, 11)))

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
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(language = "all", taxonomyFilters = List("urn:filter:6")))
    search.totalCount should be(2)
    search.results.map(_.id) should be(Seq(6, 7))

    val Success(search2) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(language = "all", taxonomyFilters = List("urn:filter:2")))
    search2.totalCount should be(3)
    search2.results.map(_.id) should be(Seq(1, 3, 5))
  }

  test("That filtering for mulitple levels/filters returns resources from all") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(language = "nb", taxonomyFilters = List("urn:filter:6", "urn:filter:1")))
    search.totalCount should be(4)
    search.results.map(_.id) should be(Seq(1, 6, 7, 12))
  }

  test("That filtering for levels/filters works with spaces as well") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(language = "nb", taxonomyFilters = List("urn:filter:7")))
    search.totalCount should be(2)
    search.results.map(_.id) should be(Seq(1, 3))
  }

  test("That filtering for subjects works as expected") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(subjects = List("urn:subject:2"), language = "all"))
    search.totalCount should be(7)
    search.results.map(_.id) should be(Seq(1, 5, 5, 6, 7, 11, 12))
  }

  test("That filtering for subjects returns all drafts with any of listed subjects") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(subjects = List("urn:subject:2", "urn:subject:1")))
    search.totalCount should be(14)
    search.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 11, 12))
  }

  test("That filtering for resource-types works as expected") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(resourceTypes = List("urn:resourcetype:academicArticle")))
    search.totalCount should be(2)
    search.results.map(_.id) should be(Seq(2, 5))

    val Success(search2) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(resourceTypes = List("urn:resourcetype:subjectMaterial")))
    search2.totalCount should be(8)
    search2.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 12))

    val Success(search3) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(resourceTypes = List("urn:resourcetype:learningpath")))
    search3.totalCount should be(4)
    search3.results.map(_.id) should be(Seq(1, 2, 3, 4))
  }

  test("That filtering on multiple context-types returns every selected type") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(learningResourceTypes =
                                      List(LearningResourceType.Article, LearningResourceType.TopicArticle),
                                    language = "all"))

    search.totalCount should be(12)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13))
  }

  test("That filtering on learning-resource-type works") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(learningResourceTypes = List(LearningResourceType.Article), language = "all"))
    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(learningResourceTypes = List(LearningResourceType.TopicArticle), language = "all"))

    search.totalCount should be(7)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))

    search2.totalCount should be(5)
    search2.results.map(_.id) should be(Seq(8, 9, 10, 11, 13))
  }

  test("That filtering on multiple context-types returns every type") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(learningResourceTypes =
                                      List(LearningResourceType.Article, LearningResourceType.TopicArticle),
                                    language = "all"))

    search.totalCount should be(12)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13))
  }

  test("That filtering on learningpath learningresourcetype returns learningpaths") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(learningResourceTypes = List(LearningResourceType.LearningPath), language = "all"))

    search.totalCount should be(6)
    search.results.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6))
    search.results.map(_.url.contains("learningpath")).distinct should be(Seq(true))
  }

  test("That filtering on supportedLanguages works") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(supportedLanguages = List("en"), language = "all"))
    search.totalCount should be(7)
    search.results.map(_.id) should be(Seq(2, 3, 4, 5, 6, 10, 11))

    val Success(search2) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(supportedLanguages = List("en", "nb"), language = "all"))
    search2.totalCount should be(18)
    search2.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6, 7, 8, 9, 10, 11, 12, 13))

    val Success(search3) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(supportedLanguages = List("nb"), language = "all"))
    search3.totalCount should be(15)
    search3.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13))
  }

  test("That filtering on supportedLanguages should still prioritize the selected language") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(supportedLanguages = List("en"), language = "nb"))

    search.totalCount should be(4)
    search.results.map(_.id) should be(Seq(2, 3, 4, 11))
    search.results.map(_.title.language) should be(Seq("nb", "nb", "nb", "nb"))
  }

  test("That meta image are returned when searching") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(language = "en", withIdIn = List(10)))

    search.totalCount should be(1)
    search.results.head.id should be(10)
    search.results.head.metaImage should be(
      Some(MetaImage("http://api-gateway.ndla-local/image-api/raw/id/123", "alt", "en")))
  }

  test("That search matches notes on drafts, but not on other content") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(noteQuery = Some("kakemonster")))

    search.totalCount should be(1)
    search.results.head.id should be(5)

    val Success(search2) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(noteQuery = Some("Katter")))

    search2.totalCount should be(0)
  }

  test("That search matches notes on drafts, even if query is regular query") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(query = Some("kakemonster")))

    search.totalCount should be(1)
    search.results.head.id should be(5)
  }

  test("That filtering for topics returns every child learningResource") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(topics = List("urn:topic:1")))

    search.totalCount should be(7)

    search.results.map(_.id) should be(Seq(1, 1, 2, 2, 4, 9, 12))
  }

  test("That searching for authors works as expected") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some("Kjekspolitiet"), language = Language.AllLanguages))
    search1.totalCount should be(1)
    search1.results.map(_.id) should be(Seq(1))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some("Svims"), language = Language.AllLanguages))
    search2.totalCount should be(2)
    search2.results.map(_.id) should be(Seq(2, 5))
  }

  test("That filtering by relevance id works when no subject is specified") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = Language.AllLanguages, relevanceIds = List("urn:relevance:core")))
    search1.results.map(_.id) should be(Seq(1, 5, 6, 7, 12))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = Language.AllLanguages,
                                    relevanceIds = List("urn:relevance:supplementary")))
    search2.results.map(_.id) should be(Seq(1, 3, 12))

    val Success(search3) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = Language.AllLanguages,
                                    relevanceIds = List("urn:relevance:supplementary", "urn:relevance:core")))
    search3.results.map(_.id) should be(Seq(1, 3, 5, 6, 7, 12))
  }

  test("That filtering by relevance and subject only returns for relevances in filtered subjects") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = Language.AllLanguages,
                                    relevanceIds = List("urn:relevance:core"),
                                    subjects = List("urn:subject:1")))

    search1.results.map(_.id) should be(Seq(1, 5))
  }

  test("That filtering by relevance and levels only returns for relevances in filtered levels") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = Language.AllLanguages,
                                    relevanceIds = List("urn:relevance:supplementary"),
                                    taxonomyFilters = List("urn:filter:7")))

    search.results.map(_.id) should be(Seq(3))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val ids = idsForLang("all").sorted.sliding(pageSize, pageSize).toList

    val Success(initialSearch) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = Language.AllLanguages, pageSize = pageSize))

    val Success(scroll1) = multiDraftSearchService.scroll(initialSearch.scrollId.get, "all", fallback = true)
    val Success(scroll2) = multiDraftSearchService.scroll(scroll1.scrollId.get, "all", fallback = true)
    val Success(scroll3) = multiDraftSearchService.scroll(scroll2.scrollId.get, "all", fallback = true)
    val Success(scroll4) = multiDraftSearchService.scroll(scroll3.scrollId.get, "all", fallback = true)
    val Success(scroll5) = multiDraftSearchService.scroll(scroll4.scrollId.get, "all", fallback = true)
    val Success(scroll6) = multiDraftSearchService.scroll(scroll5.scrollId.get, "all", fallback = true)
    val Success(scroll7) = multiDraftSearchService.scroll(scroll6.scrollId.get, "all", fallback = true)
    val Success(scroll8) = multiDraftSearchService.scroll(scroll7.scrollId.get, "all", fallback = true)
    val Success(scroll9) = multiDraftSearchService.scroll(scroll8.scrollId.get, "all", fallback = true)
    val Success(scroll10) = multiDraftSearchService.scroll(scroll9.scrollId.get, "all", fallback = true)

    initialSearch.results.map(_.id) should be(ids.head)
    scroll1.results.map(_.id) should be(ids(1))
    scroll2.results.map(_.id) should be(ids(2))
    scroll3.results.map(_.id) should be(ids(3))
    scroll4.results.map(_.id) should be(ids(4))
    scroll5.results.map(_.id) should be(ids(5))
    scroll6.results.map(_.id) should be(ids(6))
    scroll7.results.map(_.id) should be(ids(7))
    scroll8.results.map(_.id) should be(ids(8))
    scroll9.results.map(_.id) should be(List.empty)
    scroll10.results.map(_.id) should be(List.empty)
  }

  test("Filtering for statuses should only return drafts with the specified statuses") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = Language.AllLanguages,
        statusFilter = List(ArticleStatus.PROPOSAL),
        learningResourceTypes = List(LearningResourceType.Article, LearningResourceType.TopicArticle)
      ))
    search1.results.map(_.id) should be(Seq(10, 11))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = Language.AllLanguages,
        statusFilter = List(ArticleStatus.IMPORTED),
        learningResourceTypes = List(LearningResourceType.Article, LearningResourceType.TopicArticle)
      ))
    search2.results.map(_.id) should be(Seq(12))
  }

  test("Filtering for statuses should also filter learningPaths") {
    val expectedArticleIds = List(10, 11).map(_.toLong)
    val expectedIds = (expectedArticleIds).sorted

    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = Language.AllLanguages,
        statusFilter = List(ArticleStatus.PROPOSAL)
      ))
    search1.results.map(_.id) should be(expectedIds)

  }

  test("Filtering for learningresourcetype should still work if no context") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        learningResourceTypes = List(LearningResourceType.TopicArticle)
      ))

    search1.results.map(_.id) should be(Seq(8, 9, 11, 13))
    search1.results.last.contexts should be(List.empty)

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some("kek"),
        language = Language.AllLanguages,
        learningResourceTypes = List(LearningResourceType.LearningPath)
      ))

    search2.results.map(_.id) should be(Seq(6))
    search2.results.last.contexts should be(List.empty)
  }

  test("That search matches previous notes on drafts") {
    val Success(search1) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(query = Some("kultgammeltnotat")))
    val Success(search2) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(noteQuery = Some("kultgammeltnotat")))

    search1.totalCount should be(1)
    search1.results.head.id should be(5)

    search2.totalCount should be(1)
    search2.results.head.id should be(5)
  }

  test("That filtering on grepCodes returns articles which has grepCodes") {
    val Success(search1) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(grepCodes = List("K123")))
    val Success(search2) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(grepCodes = List("K456")))
    val Success(search3) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(grepCodes = List("K123", "K456")))

    search1.results.map(_.id) should be(Seq(1, 2, 3))
    search2.results.map(_.id) should be(Seq(1, 2, 5))
    search3.results.map(_.id) should be(Seq(1, 2, 3, 5))
  }

  test("ARCHIVED drafts should only be returned if filtered by ARCHIVED") {
    val query = Some("Slettet")
    val Success(search1) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(
          query = query,
          statusFilter = List(ArticleStatus.ARCHIVED)
        ))
    val Success(search2) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(
          query = query,
          statusFilter = List.empty
        ))

    search1.results.map(_.id) should be(Seq(14))
    search2.results.map(_.id) should be(Seq.empty)
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
