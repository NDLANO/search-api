/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.integration.Elastic4sClientFactory
import no.ndla.searchapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.searchapi.{IntegrationSuite, TestData, TestEnvironment}
import org.json4s.native.Serialization.read
import org.scalatest.Outcome

import scala.util.Success

class ArticleIndexServiceTest extends IntegrationSuite with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val articleIndexService = new ArticleIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService
  implicit val formats = SearchableLanguageFormats.JSonFormats

  test("That articles are indexed correctly") {
    articleIndexService.createIndexWithGeneratedName

    articleIndexService.indexDocument(article5, TestData.taxonomyTestBundle)
    articleIndexService.indexDocument(article6, TestData.taxonomyTestBundle)
    articleIndexService.indexDocument(article7, TestData.taxonomyTestBundle)

    blockUntil(() => { articleIndexService.countDocuments == 3 })

    val Success(response) = e4sClient.execute {
      search(articleIndexService.searchIndex)
    }

    val sources = response.result.hits.hits.map(_.sourceAsString)
    val articles = sources.map(source => read[SearchableArticle](source))

    val Success(expectedArticle5) =
      searchConverterService.asSearchableArticle(article5, TestData.taxonomyTestBundle)
    val Success(expectedArticle6) =
      searchConverterService.asSearchableArticle(article6, TestData.taxonomyTestBundle)
    val Success(expectedArticle7) =
      searchConverterService.asSearchableArticle(article7, TestData.taxonomyTestBundle)

    val Some(actualArticle5) = articles.find(p => p.id == article5.id.get)
    val Some(actualArticle6) = articles.find(p => p.id == article6.id.get)
    val Some(actualArticle7) = articles.find(p => p.id == article7.id.get)

    actualArticle5 should be(expectedArticle5)
    actualArticle6 should be(expectedArticle6)
    actualArticle7 should be(expectedArticle7)
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
