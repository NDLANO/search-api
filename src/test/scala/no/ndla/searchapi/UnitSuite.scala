/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("SEARCH_SERVER", "some-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setPropEnv("ARTICLE_SEARCH_INDEX_NAME", "searchapi-tests-articles")
  setPropEnv("DRAFT_SEARCH_INDEX_NAME", "searchapi-tests-drafts")
  setPropEnv("LEARNINGPATH_SEARCH_INDEX_NAME", "searchapi-tests-learningpaths")

  setPropEnv("ARTICLE_API_META_USER_NAME", "")
  setPropEnv("ARTICLE_API_META_PASSWORD", "")
  setPropEnv("ARTICLE_API_META_SERVER", "")
  setPropEnv("ARTICLE_API_META_PORT", "")
  setPropEnv("ARTICLE_API_META_RESOURCE", "")
  setPropEnv("ARTICLE_API_META_SCHEMA", "")

  setPropEnv("DRAFT_API_META_USER_NAME", "")
  setPropEnv("DRAFT_API_META_PASSWORD", "")
  setPropEnv("DRAFT_API_META_SERVER", "")
  setPropEnv("DRAFT_API_META_PORT", "")
  setPropEnv("DRAFT_API_META_RESOURCE", "")
  setPropEnv("DRAFT_API_META_SCHEMA", "")

  setPropEnv("LEARNINGPATH_API_META_USER_NAME", "")
  setPropEnv("LEARNINGPATH_API_META_PASSWORD", "")
  setPropEnv("LEARNINGPATH_API_META_SERVER", "")
  setPropEnv("LEARNINGPATH_API_META_PORT", "")
  setPropEnv("LEARNINGPATH_API_META_RESOURCE", "")
  setPropEnv("LEARNINGPATH_API_META_SCHEMA", "")
}
