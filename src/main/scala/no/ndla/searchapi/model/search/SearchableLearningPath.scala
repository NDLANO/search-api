/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.api.learningpath.Copyright
import no.ndla.searchapi.model.domain.SearchableTaxonomyContext
import org.joda.time.DateTime

case class SearchableLearningPath(id: Long,
                                  title: SearchableLanguageValues,
                                  description: SearchableLanguageValues,
                                  coverPhotoId: Option[String],
                                  duration: Option[Int],
                                  status: String,
                                  verificationStatus: String,
                                  lastUpdated: DateTime,
                                  defaultTitle: Option[String],
                                  tags: SearchableLanguageList,
                                  learningsteps: List[SearchableLearningStep],
                                  license: Copyright,
                                  isBasedOn: Option[Long],
                                  contexts: List[SearchableTaxonomyContext])

object LanguagelessSearchableLearningPath {
  case class LanguagelessSearchableLearningPath(
      id: Long,
      coverPhotoUrl: Option[String],
      duration: Option[Int],
      status: String,
      verificationStatus: String,
      lastUpdated: DateTime,
      defaultTitle: Option[String],
      learningsteps: List[SearchableLearningStep],
      license: Copyright,
      isBasedOn: Option[Long],
      contexts: List[SearchableTaxonomyContext])

  def apply(searchableLearningPath: SearchableLearningPath)
    : LanguagelessSearchableLearningPath = {
    LanguagelessSearchableLearningPath(
      searchableLearningPath.id,
      searchableLearningPath.coverPhotoId,
      searchableLearningPath.duration,
      searchableLearningPath.status,
      searchableLearningPath.verificationStatus,
      searchableLearningPath.lastUpdated,
      searchableLearningPath.defaultTitle,
      searchableLearningPath.learningsteps,
      searchableLearningPath.license,
      searchableLearningPath.isBasedOn,
      searchableLearningPath.contexts
    )
  }

}
