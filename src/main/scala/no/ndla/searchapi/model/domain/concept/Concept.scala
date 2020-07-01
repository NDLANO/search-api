/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.concept

import no.ndla.searchapi.model.domain.{Content, Copyright, MetaImage, Tag, Title}
import org.joda.time.DateTime

case class Concept(
    id: Option[Long],
    revision: Option[Int],
    title: Seq[Title],
    content: Seq[Content],
    copyright: Option[Copyright],
    source: Option[String],
    created: DateTime,
    updated: DateTime,
    updatedBy: Seq[String],
    metaImage: Seq[MetaImage],
    tags: Seq[Tag],
    subjectIds: Set[String],
    articleId: Option[Long],
    status: Status
)
