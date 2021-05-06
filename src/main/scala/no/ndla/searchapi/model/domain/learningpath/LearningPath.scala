/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.searchapi.SearchApiProperties.DatabaseDetails
import no.ndla.searchapi.SearchApiProperties.DatabaseDetails.{DatabaseDetails, LearningpathApi}
import no.ndla.searchapi.model.domain.{Content, NDLASQLSupport, Tag, Title}
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

object LearningPath extends NDLASQLSupport[LearningPath] {
  override val dbInfo: DatabaseDetails = LearningpathApi
  override val tableName = "learningpaths"

  override val jsonEncoder: Formats = DefaultFormats.withLong +
    new EnumNameSerializer(LearningPathStatus) +
    new EnumNameSerializer(LearningPathVerificationStatus)

  override val repositorySerializer: Formats = jsonEncoder + FieldSerializer[LearningPath](
    ignore("id") orElse
      ignore("learningsteps") orElse
      ignore("externalId") orElse
      ignore("revision")
  )

  override def fromResultSet(rn: ResultName[LearningPath])(rs: WrappedResultSet): Option[LearningPath] = {
    implicit val formats: Formats = jsonEncoder
    rs.stringOpt(rn.c("document"))
      .map(jsonString => {
        val meta = Serialization.read[LearningPath](jsonString)
        meta.copy(
          id = Some(rs.long(rn.c("id"))),
          revision = Some(rs.int(rn.c("revision"))),
          externalId = rs.stringOpt(rn.c("external_id"))
        )
      })
  }

}
