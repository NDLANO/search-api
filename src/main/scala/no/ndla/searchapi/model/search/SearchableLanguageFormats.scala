/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import org.json4s.Formats

object SearchableLanguageFormats {

  val JSonFormats: Formats =
    org.json4s.DefaultFormats +
      new SearchableArticleSerializer +
      new SearchableTaxonomyContextSerializer +
      new SearchableLearningStepSerializer +
      new SearchableLearningPathSerializer +
      new SearchableDraftSerializer ++
      org.json4s.ext.JodaTimeSerializers.all
}
