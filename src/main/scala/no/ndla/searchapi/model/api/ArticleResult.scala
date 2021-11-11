/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import no.ndla.searchapi.model.api.article.ArticleIntroduction
import org.joda.time.DateTime
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Search result for article api")
case class ArticleResult(
    @(ApiModelProperty @field)(description = "The unique id of this article") id: Long,
    @(ApiModelProperty @field)(description = "The title of the article") title: Title,
    @(ApiModelProperty @field)(description = "The introduction of the article") introduction: Option[
      ArticleIntroduction],
    @(ApiModelProperty @field)(description = "The type of the article") articleType: String,
    @(ApiModelProperty @field)(description = "List of supported languages") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "The time and date of last update") lastUpdated: DateTime,
    @(ApiModelProperty @field)(description = "Describes the license of the article") license: Option[String])
