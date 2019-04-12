/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.{Content, Tag, Title}
import org.joda.time.DateTime

case class Draft(
    id: Option[Long],
    revision: Option[Int],
    status: Status,
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
    published: DateTime,
    articleType: LearningResourceType.Value,
    notes: List[EditorNote]
) extends Content
