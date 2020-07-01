/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

import no.ndla.searchapi.model.domain.{Content, ContentWithId, Copyright, MetaImage, Tag, Title, article}
import org.joda.time.DateTime

case class Draft(
    id: Option[Long],
    revision: Option[Int],
    status: Status,
    title: Seq[Title],
    content: Seq[Content],
    copyright: Option[Copyright],
    tags: Seq[Tag],
    requiredLibraries: Seq[article.RequiredLibrary],
    visualElement: Seq[article.VisualElement],
    introduction: Seq[article.ArticleIntroduction],
    metaDescription: Seq[article.MetaDescription],
    metaImage: Seq[MetaImage],
    created: DateTime,
    updated: DateTime,
    updatedBy: String,
    published: DateTime,
    articleType: article.LearningResourceType.Value,
    notes: List[EditorNote],
    previousVersionsNotes: List[EditorNote],
    grepCodes: Seq[String]
) extends ContentWithId
