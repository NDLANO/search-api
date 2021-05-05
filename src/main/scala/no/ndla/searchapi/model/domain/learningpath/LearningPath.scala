/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.searchapi.SearchApiProperties.DatabaseDetails
import no.ndla.searchapi.model.domain.{Content, Tag, Title}
import org.joda.time.DateTime
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.FieldSerializer.ignore
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization
import scalikejdbc._

case class LearningPath(id: Option[Long],
                        revision: Option[Int],
                        externalId: Option[String],
                        isBasedOn: Option[Long],
                        title: Seq[Title],
                        description: Seq[Description],
                        coverPhotoId: Option[String],
                        duration: Option[Int],
                        status: LearningPathStatus.Value,
                        verificationStatus: LearningPathVerificationStatus.Value,
                        lastUpdated: DateTime,
                        tags: List[Tag],
                        owner: String,
                        copyright: Copyright,
                        learningsteps: Option[Seq[LearningStep]] = None)
    extends Content

object LearningPathStatus extends Enumeration {
  val PUBLISHED, PRIVATE, DELETED, SUBMITTED, UNLISTED = Value
}

object LearningPathVerificationStatus extends Enumeration {
  val EXTERNAL, CREATED_BY_NDLA, VERIFIED_BY_NDLA = Value
}

object LearningPath extends SQLSyntaxSupport[LearningPath] {

  val jsonSerializer = List(
    new EnumNameSerializer(LearningPathStatus),
    new EnumNameSerializer(LearningPathVerificationStatus)
  )

  val repositorySerializer = jsonSerializer :+ FieldSerializer[LearningPath](
    ignore("id") orElse
      ignore("learningsteps") orElse
      ignore("externalId") orElse
      ignore("revision")
  )

  val jsonEncoder: Formats = DefaultFormats ++ jsonSerializer

  override val tableName = "learningpaths"
  override val schemaName: Option[String] = Some(DatabaseDetails.LearningpathApi.schema)

  def apply(lp: SyntaxProvider[LearningPath])(rs: WrappedResultSet): LearningPath = apply(lp.resultName)(rs)

  def apply(lp: ResultName[LearningPath])(rs: WrappedResultSet): LearningPath = {
    implicit val formats = jsonEncoder
    val meta = Serialization.read[LearningPath](rs.string(lp.c("document")))
    meta.copy(
      id = Some(rs.long(lp.c("id"))),
      revision = Some(rs.int(lp.c("revision"))),
      externalId = rs.stringOpt(lp.c("external_id"))
    )
  }

}
