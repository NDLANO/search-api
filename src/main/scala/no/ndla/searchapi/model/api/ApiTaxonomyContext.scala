/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Taxonomy context for the resource")
case class ApiTaxonomyContext(@(ApiModelProperty@field)(description = "Id of the taoxonomy object") id: String,
                              @(ApiModelProperty@field)(description = "Name of the subject this context is in") subject: String,
                              @(ApiModelProperty@field)(description = "Path to the resource in this context") path: String,
                              @(ApiModelProperty@field)(description = "Breadcrumbs of path to the resource in this context") breadcrumbs: List[String],
                              @(ApiModelProperty@field)(description = "Filters connected to this object and subject") filters: List[ContextFilter],
                              @(ApiModelProperty@field)(description = "Type in this context.") learningResourceType: String,
                              @(ApiModelProperty@field)(description = "Resource-types of this context.") resourceTypes: List[String],
                              @(ApiModelProperty@field)(description = "Language for this context") language: String)
