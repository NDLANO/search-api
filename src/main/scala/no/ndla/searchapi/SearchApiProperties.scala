/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.Domains
import no.ndla.network.secrets.Secrets.readSecrets

import scala.util.Properties._
import scala.util.{Failure, Success}

object SearchApiProperties extends LazyLogging {
  val ApplicationPort = 80
  val ContactEmail = "christergundersen@ndla.no"
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")

  val DefaultLanguage = "nb"

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  lazy val Domain = Domains.get(Environment)

  def booleanProp(key: String) = prop(key).toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = envOrElse(key, default)
}
