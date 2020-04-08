/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

import no.ndla.searchapi.model.domain.{Content, Tag, Title}
import org.joda.time.DateTime
import org.json4s.FieldSerializer

case class Article(
    id: Option[Long],
    revision: Option[Int],
    title: Seq[Title],
    content: Seq[ArticleContent],
    copyright: Copyright,
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
    grepCodes: Seq[String]
) extends Content

object LearningResourceType extends Enumeration {
  val Article = Value("standard")
  val TopicArticle = Value("topic-article")
  val LearningPath = Value("learningpath")

  def all = LearningResourceType.values.map(_.toString).toList

  def valueOf(s: String): Option[LearningResourceType.Value] = LearningResourceType.values.find(_.toString == s)
}
