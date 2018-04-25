/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

case class SearchableLearningStep(stepType: String,
                                  title: SearchableLanguageValues,
                                  description: SearchableLanguageValues)
