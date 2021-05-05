package no.ndla.searchapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties.IndexBulkSize
import no.ndla.searchapi.integration.DataSources
import no.ndla.searchapi.model.domain.article.Article
import math.ceil
import scalikejdbc._

trait ArticleRepository {
  this: DataSources =>
  val articleRepository: ArticleRepository

  class ArticleRepository extends LazyLogging with Repository[Article] {
    private val DBName = Symbol("article-api")

    override def getByPage(pageSize: Int, offset: Int)(
        implicit session: DBSession = ReadOnlyAutoSession): Seq[Article] = {
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

    override protected def documentCount(implicit session: DBSession = ReadOnlyAutoSession): Long = {
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

    override def pageCount(pageSize: Int): Int = {
      val dbCount = documentCount
      val pageSize = IndexBulkSize
      ceil(dbCount.toDouble / pageSize.toDouble).toInt
    }
  }
}
