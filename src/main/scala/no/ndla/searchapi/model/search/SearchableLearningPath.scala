/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.util.Date

import no.ndla.searchapi.model.api.learningpath.Copyright

case class SearchableLearningPath(
    id: Long,
    titles: SearchableLanguageValues,
    descriptions: SearchableLanguageValues,
    coverPhotoUrl: Option[String],
    duration: Option[Int],
    status: String,
    verificationStatus: String,
    lastUpdated: Date,
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    learningsteps: Seq[SearchableLearningStep],
    license: Copyright,
    isBasedOn: Option[Long]) extends Searchable
