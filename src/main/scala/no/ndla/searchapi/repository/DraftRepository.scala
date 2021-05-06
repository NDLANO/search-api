package no.ndla.searchapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties.DatabaseDetails
import no.ndla.searchapi.integration.DataSources
import no.ndla.searchapi.model.domain.draft.Draft
import no.ndla.searchapi.model.domain.learningpath.{LearningPath, LearningPathStatus, LearningStep, StepStatus}
import scalikejdbc._

trait DraftRepository {
  this: DataSources =>
  val draftRepository: DraftRepository

  class DraftRepository extends LazyLogging with Repository[Draft] {
    override val connectionPoolName: Symbol = DatabaseDetails.DraftApi.connectionPoolName
    override def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession): Seq[Draft] = {
      val ar = Draft.syntax("ar")
      sql"""
           select *
           from (select
                   ${ar.result.*},
                   ${ar.revision} as revision,
                   max(revision) over (partition by article_id) as max_revision
                 from ${Draft.as(ar)}
                 where document is not NULL) _
           where revision = max_revision
           offset $offset
           limit $pageSize
      """
        .map(Draft.fromResultSet(ar))
        .list()
        .apply()
        .flatten
    }

    override protected def documentCount(implicit session: DBSession): Long =
      sql"select count(distinct article_id) from ${Draft.table} where document is not NULL"
        .map(rs => rs.long("count"))
        .single()
        .apply()
        .getOrElse(0)
  }
}
