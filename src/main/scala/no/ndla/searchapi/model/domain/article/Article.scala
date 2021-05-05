/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

import no.ndla.searchapi.SearchApiProperties.DatabaseDetails
import no.ndla.searchapi.model.domain.draft.Availability
import no.ndla.searchapi.model.domain.{Content, Tag, Title}
import org.joda.time.DateTime
import org.json4s.FieldSerializer.ignore
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

case class Article(id: Option[Long],
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
                   published: DateTime,
                   articleType: LearningResourceType.Value,
                   grepCodes: Seq[String])
    extends Content

object LearningResourceType extends Enumeration {
  val Article: LearningResourceType.Value = Value("standard")
  val TopicArticle: LearningResourceType.Value = Value("topic-article")
  val LearningPath: LearningResourceType.Value = Value("learningpath")

  def all: List[String] = LearningResourceType.values.map(_.toString).toList

  def valueOf(s: String): Option[LearningResourceType.Value] = LearningResourceType.values.find(_.toString == s)
}

object Article extends SQLSyntaxSupport[Article] {

  val jsonEncoder: Formats = DefaultFormats.withLong +
    new EnumNameSerializer(LearningResourceType) +
    new EnumNameSerializer(Availability)

  override val tableName = "contentdata"
  override lazy val schemaName: Option[String] = Some(DatabaseDetails.ArticleApi.schema)

  def fromResultSet(lp: SyntaxProvider[Article])(rs: WrappedResultSet): Option[Article] =
    fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Article])(rs: WrappedResultSet): Option[Article] = {
    implicit val formats: Formats = repositorySerializer

    rs.stringOpt(lp.c("document"))
      .map(jsonStr => {
        val meta = Serialization.read[Article](jsonStr)
        meta.copy(
          id = Some(rs.long(lp.c("article_id"))),
          revision = Some(rs.int(lp.c("revision")))
        )
      })
  }

  val repositorySerializer: Formats = jsonEncoder +
    FieldSerializer[Article](
      ignore("id")
    )
}
