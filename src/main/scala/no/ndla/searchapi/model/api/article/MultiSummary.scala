/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.searchapi.model.api.Subject
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the article")
case class MultiSummary(@(ApiModelProperty@field)(description = "The unique id of the article") id: Long,
                          @(ApiModelProperty@field)(description = "The title of the article") title: ArticleTitle,
                          @(ApiModelProperty@field)(description = "A metaDescription for the article") metaDescription: Option[ArticleMetaDescription],
                          @(ApiModelProperty@field)(description = "The full url to where the complete information about the article can be found") url: String,
                          @(ApiModelProperty@field)(description = "The type of article this is.") contentTypes: Seq[String],
                          @(ApiModelProperty@field)(description = "The type of resource this article is.") resourceTypes: Seq[String],
                          @(ApiModelProperty@field)(description = "The subjects this article is found in.") subjects: Seq[Subject],
                          @(ApiModelProperty@field)(description = "A list of available languages for this article") supportedLanguages: Seq[String])
