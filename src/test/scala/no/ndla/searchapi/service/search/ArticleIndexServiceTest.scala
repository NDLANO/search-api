/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.http.ElasticDsl._
import no.ndla.searchapi.integration.{Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import org.json4s.native.Serialization.read
import java.nio.file.{Files, Path}

import scala.util.Success


class ArticleIndexServiceTest extends UnitSuite with TestEnvironment {
  val tmpDir: Path = Files.createTempDirectory(this.getClass.getName)
  val localNodeSettings: Map[String, String] = LocalNode.requiredSettings(this.getClass.getName, tmpDir.toString)
  val localNode = LocalNode(localNodeSettings)

  override val e4sClient = NdlaE4sClient(localNode.http(true))
  override val articleIndexService = new ArticleIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService
  implicit val formats = SearchableLanguageFormats.JSonFormats

  override def afterAll(): Unit = {
    localNode.stop(true)
  }

  test("That articles are indexed correctly") {
    articleIndexService.createIndexWithGeneratedName

    articleIndexService.indexDocument(article5, Some(TestData.taxonomyTestBundle))
    articleIndexService.indexDocument(article6, Some(TestData.taxonomyTestBundle))
    articleIndexService.indexDocument(article7, Some(TestData.taxonomyTestBundle))

    blockUntil(() => {articleIndexService.countDocuments == 3})

    val Success(response) = e4sClient.execute{
      search(articleIndexService.searchIndex)
    }

    val sources = response.result.hits.hits.map(_.sourceAsString)
    val articles = sources.map(source => read[SearchableArticle](source))

    val Success(expectedArticle5) = searchConverterService.asSearchableArticle(article5, Some(TestData.taxonomyTestBundle))
    val Success(expectedArticle6) = searchConverterService.asSearchableArticle(article6, Some(TestData.taxonomyTestBundle))
    val Success(expectedArticle7) = searchConverterService.asSearchableArticle(article7, Some(TestData.taxonomyTestBundle))

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
