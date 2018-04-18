/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.searchapi.model.api.{AccessDeniedException, ValidationException, ValidationMessage}
import no.ndla.searchapi.model.domain.article.MetaDescription
import no.ndla.searchapi.model.domain.{Content, Tag, Title}
import org.joda.time.DateTime

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
                        learningsteps: List[LearningStep] = Nil)
    extends Content

object LearningPathStatus extends Enumeration {
  val PUBLISHED, PRIVATE, DELETED = Value

  def valueOf(s: String): Option[LearningPathStatus.Value] = {
    LearningPathStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrError(status: String): LearningPathStatus.Value = {
    valueOf(status) match {
      case Some(status) => status
      case None =>
        throw new ValidationException(
          errors = List(ValidationMessage("status", s"'$status' is not a valid publishingstatus.")))
    }
  }

  def valueOfOrDefault(s: String): LearningPathStatus.Value = {
    valueOf(s).getOrElse(LearningPathStatus.PRIVATE)
  }
}

object LearningPathVerificationStatus extends Enumeration {
  val EXTERNAL, CREATED_BY_NDLA, VERIFIED_BY_NDLA = Value

  def valueOf(s: String): Option[LearningPathVerificationStatus.Value] = {
    LearningPathVerificationStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrDefault(s: String): LearningPathVerificationStatus.Value = {
    valueOf(s).getOrElse(LearningPathVerificationStatus.EXTERNAL)
  }
}
