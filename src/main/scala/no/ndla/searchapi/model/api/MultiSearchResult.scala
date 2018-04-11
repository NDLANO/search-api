/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about search-results")
case class MultiSearchResult(@(ApiModelProperty@field)(description = "The total number of resources matching this query") totalCount: Long,
                             @(ApiModelProperty@field)(description = "Number of total matched learningpaths") totalCountLearningPaths: Long,
                             @(ApiModelProperty@field)(description = "Number of total matched 'fagstoff'") totalCountSubjectMaterial: Long,
                             @(ApiModelProperty@field)(description = "Number of total matched 'oppgaver'") totalCountTasks: Long,
                             @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                             @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                             @(ApiModelProperty@field)(description = "The chosen search language") language: String,
                             @(ApiModelProperty@field)(description = "The search results") results: Seq[MultiSearchSummary])