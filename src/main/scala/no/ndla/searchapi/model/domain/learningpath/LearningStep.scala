/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.searchapi.SearchApiProperties.DatabaseDetails
import no.ndla.searchapi.SearchApiProperties.DatabaseDetails.{DatabaseDetails, LearningpathApi}
import no.ndla.searchapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.searchapi.model.domain.{NDLASQLSupport, Title}
import org.json4s.FieldSerializer.ignore
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

case class LearningStep(id: Option[Long],
                        revision: Option[Int],
                        externalId: Option[String],
                        learningPathId: Option[Long],
                        seqNo: Int,
                        title: Seq[Title],
                        description: Seq[Description],
                        embedUrl: Seq[EmbedUrl],
                        `type`: StepType.Value,
                        license: Option[String],
                        showTitle: Boolean = false,
                        status: StepStatus.Value = StepStatus.ACTIVE) {}

object StepStatus extends Enumeration {

  val ACTIVE, DELETED = Value

  def valueOf(s: String): Option[StepStatus.Value] = {
    StepStatus.values.find(_.toString == s)
  }

  def valueOfOrError(status: String): StepStatus.Value = {
    valueOf(status) match {
      case Some(s) => s
      case None =>
        throw new ValidationException(errors = List(ValidationMessage("status", s"'$status' is not a valid status.")))
    }
  }

  def valueOfOrDefault(s: String): StepStatus.Value = {
    valueOf(s).getOrElse(StepStatus.ACTIVE)
  }
}

object StepType extends Enumeration {
  val INTRODUCTION, TEXT, QUIZ, TASK, MULTIMEDIA, SUMMARY, TEST = Value

  def valueOf(s: String): Option[StepType.Value] = {
    StepType.values.find(_.toString == s)
  }

  def valueOfOrError(s: String): StepType.Value = {
    valueOf(s) match {
      case Some(stepType) => stepType
      case None =>
        throw new ValidationException(errors = List(ValidationMessage("type", s"'$s' is not a valid steptype.")))
    }
  }

  def valueOfOrDefault(s: String): StepType.Value = {
    valueOf(s).getOrElse(StepType.TEXT)
  }
}

object LearningStep extends NDLASQLSupport[LearningStep] {
  override val dbInfo: DatabaseDetails = LearningpathApi
  override val tableName = "learningsteps"

  override val jsonEncoder = DefaultFormats.withLong +
    new EnumNameSerializer(StepType) +
    new EnumNameSerializer(StepStatus) +
    new EnumNameSerializer(EmbedType)

  override val repositorySerializer = jsonEncoder + FieldSerializer[LearningStep](
    serializer =
      ignore("id") orElse
        ignore("learningPathId") orElse
        ignore("externalId") orElse
        ignore("revision")
  )

  override def fromResultSet(rn: ResultName[LearningStep])(rs: WrappedResultSet): Option[LearningStep] = {
    implicit val formats: Formats = jsonEncoder

    rs.stringOpt(rn.c("document"))
      .map(jsonString => {
        val meta = Serialization.read[LearningStep](jsonString)
        LearningStep(
          Some(rs.long(rn.c("id"))),
          Some(rs.int(rn.c("revision"))),
          rs.stringOpt(rn.c("external_id")),
          Some(rs.long(rn.c("learning_path_id"))),
          meta.seqNo,
          meta.title,
          meta.description,
          meta.embedUrl,
          meta.`type`,
          meta.license,
          meta.showTitle,
          meta.status
        )
      })
  }
}
