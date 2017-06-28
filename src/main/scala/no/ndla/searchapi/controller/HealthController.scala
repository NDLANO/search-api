/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import org.scalatra.{Ok, ScalatraServlet}

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    get("/") {
      Ok()
    }
  }
}
