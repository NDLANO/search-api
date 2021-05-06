package no.ndla.searchapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties.{DatabaseDetails, IndexBulkSize}
import no.ndla.searchapi.integration.DataSources
import no.ndla.searchapi.model.domain.article.Article

import math.ceil
import scalikejdbc._

trait ArticleRepository {
  this: DataSources =>
  val articleRepository: ArticleRepository

  class ArticleRepository extends LazyLogging with Repository[Article] {
    override val connectionPoolName: Symbol = DatabaseDetails.ArticleApi.connectionPoolName
    override def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession): Seq[Article] = {
      val ar = Article.syntax("ar")
      sql"""
           select *
           from (select
                   ${ar.result.*},
                   ${ar.revision} as revision,
                   max(revision) over (partition by article_id) as max_revision
                 from ${Article.as(ar)}
                 where document is not NULL) _
           where revision = max_revision
           offset $offset
           limit $pageSize
      """
        .map(Article.fromResultSet(ar))
        .list()
        .apply()
        .flatten
    }

    override protected def documentCount(implicit session: DBSession): Long = {
      sql"""
           select count(distinct article_id)
           from (select
                   *,
                   max(revision) over (partition by article_id) as max_revision
                 from ${Article.table}
                 where document is not NULL) _
           where revision = max_revision
         """
        .map(rs => rs.long("count"))
        .single()
        .apply()
        .getOrElse(0)
    }
  }
}
