package no.ndla.searchapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.integration.DataSources
import no.ndla.searchapi.model.domain.article.Article
import no.ndla.searchapi.model.domain.learningpath.{LearningPath, LearningPathStatus, LearningStep, StepStatus}
import scalikejdbc._

trait LearningpathRepository {
  this: DataSources =>
  val learningpathRepository: LearningpathRepository

  class LearningpathRepository extends LazyLogging with Repository[LearningPath] {
    override def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession): Seq[LearningPath] = {

      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      val lps = SubQuery.syntax("lps").include(lp)
      sql"""
            select ${lps.resultAll}, ${ls.resultAll} from (select ${lp.resultAll}
                                                           from ${LearningPath.as(lp)}
                                                           where document#>>'{status}' = ${LearningPathStatus.PUBLISHED.toString}
                                                           limit $pageSize
                                                           offset $offset) lps
            left join ${LearningStep.as(ls)} on ${lps(lp).id} = ${ls.learningPathId}
      """
        .one(LearningPath(lps(lp).resultName))
        .toMany(LearningStep.opt(ls.resultName))
        .map { (learningpath, learningsteps) =>
          learningpath.copy(learningsteps = Some(learningsteps.filter(_.status == StepStatus.ACTIVE).toSeq))
        }
        .list()
        .apply()
    }

    override protected def documentCount(implicit session: DBSession): Long = ???

    override def pageCount(pageSize: Int): Int = ???
  }
}
