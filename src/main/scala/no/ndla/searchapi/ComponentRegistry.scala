/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.searchapi

import no.ndla.searchapi.controller.{HealthController, SearchController}

object ComponentRegistry
  extends SearchController
  with HealthController
{
  implicit val swagger = new SearchSwagger

  lazy val searchController = new SearchController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
}
