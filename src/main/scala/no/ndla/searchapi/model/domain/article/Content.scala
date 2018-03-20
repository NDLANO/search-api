/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

import java.util.Date

import org.json4s.FieldSerializer

sealed trait Content {
  def id: Option[Long]
}

case class Article(id: Option[Long],
                   revision: Option[Int],
                   title: Seq[ArticleTitle],
                   content: Seq[ArticleContent],
                   copyright: Copyright,
                   tags: Seq[ArticleTag],
                   requiredLibraries: Seq[RequiredLibrary],
                   visualElement: Seq[VisualElement],
                   introduction: Seq[ArticleIntroduction],
                   metaDescription: Seq[ArticleMetaDescription],
                   metaImageId: Option[String],
                   created: Date,
                   updated: Date,
                   updatedBy: String,
                   articleType: String) extends Content


object Article {
  implicit val formats = org.json4s.DefaultFormats

  val JSonSerializer = FieldSerializer[Article](
    FieldSerializer.ignore("id")
  )
}

object LearningResourceType extends Enumeration {
  val Standard = Value("standard")
  val TopicArticle = Value("topic-article")
  val LearningPath = Value("learningpath")

  def all = LearningResourceType.values.map(_.toString).toList

  def valueOf(s: String): Option[LearningResourceType.Value] = LearningResourceType.values.find(_.toString == s)
}

case class Concept(id: Option[Long],
                   title: Seq[ConceptTitle],
                   content: Seq[ConceptContent],
                   copyright: Option[Copyright],
                   created: Date,
                   updated: Date) extends Content

object Concept {
  implicit val formats = org.json4s.DefaultFormats

  val JSonSerializer = FieldSerializer[Concept](
    FieldSerializer.ignore("id")
      .orElse(FieldSerializer.ignore("revision"))
  )
}
