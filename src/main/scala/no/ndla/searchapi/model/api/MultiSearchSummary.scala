/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the resource")
case class MultiSearchSummary(@(ApiModelProperty@field)(description = "The unique id of the resource") id: Long,
                              @(ApiModelProperty@field)(description = "The title of the resource") title: Title,
                              @(ApiModelProperty@field)(description = "The meta description of the resource") metaDescription: MetaDescription,
                              @(ApiModelProperty@field)(description = "The meta image for the resource") metaImage: Option[String],
                              @(ApiModelProperty@field)(description = "Url pointing to the resource") url: String,
                              @(ApiModelProperty@field)(description = "Contexts of the resource") contexts: List[ApiTaxonomyContext],
                              @(ApiModelProperty@field)(description = "Languages the resource exists in") supportedLanguages: Seq[String])
