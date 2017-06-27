/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.searchapi.controller.{HealthController, SearchController}
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
  extends HealthController
  with SearchController
{
  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val searchController = mock[SearchController]
}
