/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

import no.ndla.searchapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.{Tag, Title}
import org.joda.time.DateTime

import scala.util.{Failure, Success, Try}

sealed trait Content {
  def id: Option[Long]
}

case class Draft(id: Option[Long],
                 revision: Option[Int],
                 status: Set[ArticleStatus.Value],
                 title: Seq[Title],
                 content: Seq[ArticleContent],
                 copyright: Option[Copyright],
                 tags: Seq[Tag],
                 requiredLibraries: Seq[RequiredLibrary],
                 visualElement: Seq[VisualElement],
                 introduction: Seq[ArticleIntroduction],
                 metaDescription: Seq[MetaDescription],
                 metaImage: Seq[ArticleMetaImage],
                 created: DateTime,
                 updated: DateTime,
                 updatedBy: String,
                 articleType: LearningResourceType.Value,
                 notes: Seq[String]) extends Content

object ArticleStatus extends Enumeration {
  val CREATED, IMPORTED, USER_TEST, QUEUED_FOR_PUBLISHING, QUALITY_ASSURED, DRAFT, SKETCH, PUBLISHED = Value

  def valueOfOrError(s: String): Try[ArticleStatus.Value] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None =>
        val validStatuses = values.map(_.toString).mkString(", ")
        Failure(new ValidationException(errors=Seq(ValidationMessage("status", s"'$s' is not a valid article status. Must be one of $validStatuses"))))
    }

  def valueOf(s: String): Option[ArticleStatus.Value] = values.find(_.toString == s.toUpperCase)
}
