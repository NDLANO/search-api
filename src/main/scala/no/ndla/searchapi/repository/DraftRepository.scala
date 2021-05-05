package no.ndla.searchapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.integration.DataSources
import no.ndla.searchapi.model.domain.draft.Draft
import no.ndla.searchapi.model.domain.learningpath.{LearningPath, LearningPathStatus, LearningStep, StepStatus}
import scalikejdbc._

trait DraftRepository {
  this: DataSources =>
  val draftRepository: DraftRepository

  class DraftRepository extends LazyLogging with Repository[Draft] {
    override def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession): Seq[Draft] = ???

    override protected def documentCount(implicit session: DBSession): Long = ???

    override def pageCount(pageSize: Int): Int = ???
  }
}
