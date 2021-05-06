package no.ndla.searchapi.integration

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import no.ndla.searchapi.SearchApiProperties.DatabaseDetails.DatabaseDetails
import no.ndla.searchapi.SearchApiProperties.{ApplicationName, DatabaseDetails, Environment}

trait DataSources {
  val articleApiDataSource: HikariDataSource
  val draftApiDataSource: HikariDataSource
  val learningpathApiDataSource: HikariDataSource
}

object DataSource {
  private def buildHikariDataSource(details: DatabaseDetails): HikariDataSource = {
    val JDBCApplicationName = s"NDLA $ApplicationName ($Environment)"
    val JDBCUrl =
      s"jdbc:postgresql://${details.server}:${details.port}/${details.database}?ApplicationName=$JDBCApplicationName"

    val dataSourceConfig = new HikariConfig()
    dataSourceConfig.setUsername(details.username)
    dataSourceConfig.setPassword(details.password)
    dataSourceConfig.setJdbcUrl(JDBCUrl)
    dataSourceConfig.setSchema(details.schema)
    dataSourceConfig.setMaximumPoolSize(10)
    new HikariDataSource(dataSourceConfig)
  }

  def ArticleApiDataSource: HikariDataSource = {
    buildHikariDataSource(DatabaseDetails.ArticleApi)
  }

  def LearningpathApiDataSource: HikariDataSource = {
    buildHikariDataSource(DatabaseDetails.LearningpathApi)
  }

  def DraftApiDataSource: HikariDataSource = {
    buildHikariDataSource(DatabaseDetails.DraftApi)
  }

}
