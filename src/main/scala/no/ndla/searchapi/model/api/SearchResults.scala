/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

sealed trait SearchResults

@ApiModel(description = "Search result for article api")
case class ArticleResults(@(ApiModelProperty@field)(description = "The type of search results") `type`: String,
                          @(ApiModelProperty@field)(description = "The actual search results") results: Seq[ArticleResult]
                         ) extends SearchResults
