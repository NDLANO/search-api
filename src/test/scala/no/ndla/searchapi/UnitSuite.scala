/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import org.joda.time.{DateTime, DateTimeUtils}
import org.mockito.scalatest.MockitoSugar
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

abstract class UnitSuite
    extends AnyFunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with PrivateMethodTester {
  setEnv("NDLA_ENVIRONMENT", "local")
  setEnv("SEARCH_SERVER", "some-server")
  setEnv("SEARCH_REGION", "some-region")
  setEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setEnv("ARTICLE_SEARCH_INDEX_NAME", "searchapi-tests-articles")
  setEnv("DRAFT_SEARCH_INDEX_NAME", "searchapi-tests-drafts")
  setEnv("LEARNINGPATH_SEARCH_INDEX_NAME", "searchapi-tests-learningpaths")

  def setEnv(key: String, value: String) = env.put(key, value)

  def setEnvIfAbsent(key: String, value: String) = env.putIfAbsent(key, value)

  private def env = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
  }

  def withFrozenTime(time: DateTime = new DateTime())(toExecute: => Any) = {
    DateTimeUtils.setCurrentMillisFixed(time.getMillis)
    toExecute
    DateTimeUtils.setCurrentMillisSystem()
  }
}
