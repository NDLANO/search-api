/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.joda.time.DateTime
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Search result for image api")
case class ImageResult(
    @(ApiModelProperty @field)(description = "The unique id of this image") id: Long,
    @(ApiModelProperty @field)(description = "The title of this image") title: Title,
    @(ApiModelProperty @field)(description = "The alt text of this image") altText: ImageAltText,
    @(ApiModelProperty @field)(description = "A direct link to the image") previewUrl: String,
    @(ApiModelProperty @field)(description = "A link to get meta data related to the image") metaUrl: String,
    @(ApiModelProperty @field)(description = "List of supported languages") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "The time and date of last update") lastUpdated: DateTime,
    @(ApiModelProperty @field)(description = "Describes the license of the image") license: Option[String])
