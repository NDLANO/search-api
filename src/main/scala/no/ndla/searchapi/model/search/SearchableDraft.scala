/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import org.joda.time.DateTime

case class SearchableDraft(id: Long,
                           title: SearchableLanguageValues,
                           content: SearchableLanguageValues,
                           visualElement: SearchableLanguageValues,
                           introduction: SearchableLanguageValues,
                           metaDescription: SearchableLanguageValues,
                           tags: SearchableLanguageList,
                           lastUpdated: DateTime,
                           license: Option[String],
                           authors: List[String],
                           articleType: String,
                           metaImage: SearchableLanguageValues,
                           defaultTitle: Option[String],
                           supportedLanguages: List[String],
                           notes: List[String],
                           contexts: List[SearchableTaxonomyContext])
