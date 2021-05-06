/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

import no.ndla.network.secrets.Database
import no.ndla.searchapi.SearchApiProperties.DatabaseDetails
import no.ndla.searchapi.SearchApiProperties.DatabaseDetails.{DatabaseDetails, DraftApi}
import no.ndla.searchapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.searchapi.model.domain.{Content, NDLASQLSupport, Tag, Title, article, draft}
import no.ndla.searchapi.model.domain.article.Article
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

object Draft extends NDLASQLSupport[Draft] {
  override val dbInfo: DatabaseDetails = DraftApi
  override val tableName = "articledata"

  override val jsonEncoder: Formats = DefaultFormats.withLong +
    new EnumNameSerializer(ArticleStatus) +
    new EnumNameSerializer(article.LearningResourceType) +
    new EnumNameSerializer(Availability)

  override val repositorySerializer: Formats = jsonEncoder +
    FieldSerializer[Draft](
      ignore("id") orElse
        ignore("revision")
    )

  override def fromResultSet(rn: ResultName[Draft])(rs: WrappedResultSet): Option[Draft] = {
    implicit val formats: Formats = jsonEncoder
    rs.stringOpt(rn.c("document"))
      .map(jsonString => {
        val meta = Serialization.read[Draft](jsonString)
        meta.copy(
          id = Some(rs.long(rn.c("article_id"))),
          revision = Some(rs.int(rn.c("revision")))
        )
      })
  }
}
