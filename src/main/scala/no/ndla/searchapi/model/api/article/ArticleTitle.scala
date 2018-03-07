/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.searchapi.model.domain.LanguageField
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Description of a title")
case class ArticleTitle(@(ApiModelProperty@field)(description = "The freetext title of the article") title: String,
                        @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: String)
  extends LanguageField[String] {
  override def value: String = title
}
