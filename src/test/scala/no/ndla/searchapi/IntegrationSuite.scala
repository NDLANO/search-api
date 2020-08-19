package no.ndla.searchapi

import org.testcontainers.elasticsearch.ElasticsearchContainer

import scala.util.Try

abstract class IntegrationSuite extends UnitSuite {

  val elasticSearchContainer = Try {
    val searchEngineVersion = "d63726c" // elasticsearch 6.8.4
    val c = new ElasticsearchContainer(
      s"950645517739.dkr.ecr.eu-central-1.amazonaws.com/ndla/search-engine:$searchEngineVersion")
    c.start()
    c
  }
  val elasticSearchHost = elasticSearchContainer.map(c => s"http://${c.getHttpHostAddress}")
}
