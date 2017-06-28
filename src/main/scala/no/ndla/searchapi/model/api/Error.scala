/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.searchapi.model.api

import no.ndla.searchapi.SearchApiProperties
import java.util.Date
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Information about an error")
case class Error(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String = Error.GENERIC,
                 @(ApiModelProperty@field)(description = "Description of the error") description: String = Error.GENERIC_DESCRIPTION,
                 @(ApiModelProperty@field)(description = "An optional id referring to the cover") id: Option[Long] = None,
                 @(ApiModelProperty@field)(description = "When the error occured") occuredAt: Date = new Date())

@ApiModel(description = "Information about validation errors")
case class ValidationError(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String = Error.VALIDATION,
                           @(ApiModelProperty@field)(description = "Description of the error") description: String = Error.VALIDATION_DESCRIPTION,
                           @(ApiModelProperty@field)(description = "List of validation messages") messages: Seq[ValidationMessage],
                           @(ApiModelProperty@field)(description = "When the error occured") occuredAt: Date = new Date())

@ApiModel(description = "A message describing a validation error on a specific field")
case class ValidationMessage(@(ApiModelProperty@field)(description = "The field the error occured in") field: String,
                             @(ApiModelProperty@field)(description = "The validation message") message: String)

object Error {
  val GENERIC = "GENERIC"
  val VALIDATION = "VALIDATION"

  val GENERIC_DESCRIPTION = s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${SearchApiProperties.ContactEmail} if the error persists."
  val VALIDATION_DESCRIPTION = "Validation Error"

  val GenericError = Error(GENERIC, GENERIC_DESCRIPTION)
}

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage]) extends RuntimeException(message)
