package no.ndla.searchapi

import org.testcontainers.elasticsearch.ElasticsearchContainer

import scala.util.Try

abstract class IntegrationSuite extends UnitSuite {

  val elasticSearchContainer = Try {
    val esVersion = "6.3.2"
    val c = new ElasticsearchContainer(s"docker.elastic.co/elasticsearch/elasticsearch:$esVersion")
    c.start()
    c
  }
  val elasticSearchHost = elasticSearchContainer.map(c => s"http://${c.getHttpHostAddress}")
}
