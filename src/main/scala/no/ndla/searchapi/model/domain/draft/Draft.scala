/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

import no.ndla.network.secrets.Database
import no.ndla.searchapi.SearchApiProperties.DatabaseDetails
import no.ndla.searchapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.searchapi.model.domain.article
import no.ndla.searchapi.model.domain.article.Article
import no.ndla.searchapi.model.domain.draft
import no.ndla.searchapi.model.domain.{Content, Tag, Title}
import org.joda.time.DateTime
import org.json4s.FieldSerializer.ignore
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization

import java.util.Date
import scala.util.{Failure, Success, Try}
import scalikejdbc._

case class Draft(
    id: Option[Long],
    revision: Option[Int],
    status: Status,
    title: Seq[Title],
    content: Seq[article.ArticleContent],
    copyright: Option[draft.Copyright],
    tags: Seq[Tag],
    requiredLibraries: Seq[article.RequiredLibrary],
    visualElement: Seq[article.VisualElement],
    introduction: Seq[article.ArticleIntroduction],
    metaDescription: Seq[article.MetaDescription],
    metaImage: Seq[article.ArticleMetaImage],
    created: DateTime,
    updated: DateTime,
    updatedBy: String,
    published: DateTime,
    articleType: article.LearningResourceType.Value,
    notes: List[EditorNote],
    previousVersionsNotes: List[EditorNote],
    grepCodes: Seq[String]
) extends Content

object Draft extends SQLSyntaxSupport[Draft] {

  val jsonEncoder: Formats = DefaultFormats.withLong +
    new EnumNameSerializer(ArticleStatus) +
    new EnumNameSerializer(article.LearningResourceType) +
    new EnumNameSerializer(Availability)

  val repositorySerializer: Formats = jsonEncoder +
    FieldSerializer[Draft](
      ignore("id") orElse
        ignore("revision")
    )

  override val tableName = "articledata"
  override lazy val schemaName = Some(DatabaseDetails.DraftApi.schema)

  def fromResultSet(lp: SyntaxProvider[Article])(rs: WrappedResultSet): Article = fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Article])(rs: WrappedResultSet): Article = {
    implicit val formats = jsonEncoder
    val meta = Serialization.read[Article](rs.string(lp.c("document")))
    meta.copy(
      id = Some(rs.long(lp.c("article_id"))),
      revision = Some(rs.int(lp.c("revision")))
    )
  }
}
