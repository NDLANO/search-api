/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.RequestSuccess
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.integration.Elastic4sClient
import org.scalatra.{InternalServerError, Ok, ScalatraServlet}

import scala.util.{Failure, Success}

trait HealthController {
  this: Elastic4sClient =>
  val healthController: HealthController

  class HealthController extends ScalatraServlet with LazyLogging {
    get("/") {
      e4sClient.execute(clusterHealth()) match {
        case Failure(exception) =>
          logger.error("Something went wrong when contacting elasticsearch instance when performing health check",
                       exception)
          InternalServerError()
        case Success(RequestSuccess(status, body, headers, result)) =>
          if (status == 200) {
            Ok()
          } else {
            logger.error(
              s"""Health check against elasticsearch failed --->
                 |Status: $status
                 |Body: ${body.getOrElse("<missing>")}
                 |headers:   ${headers.mkString("\n\t")}
                 |Result: ${result.toString}
                 |""".stripMargin
            )
            InternalServerError()
          }
      }
    }

  }
}
