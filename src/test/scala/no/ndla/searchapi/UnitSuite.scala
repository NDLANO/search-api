/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import org.scalatest.mockito.MockitoSugar
import org.scalatest._

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll with PrivateMethodTester {
  setEnv("NDLA_ENVIRONMENT", "local")

  def setEnv(key: String, value: String) = env.put(key, value)

  def setEnvIfAbsent(key: String, value: String) = env.putIfAbsent(key, value)

  private def env = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
  }
}
