/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class LearningpathApiSearchResults(totalCount: Long,
                                        page: Int,
                                        pageSize: Int,
                                        language: String,
                                        results: Seq[LearningpathApiSearchResult])

case class LearningpathApiSearchResult(id: Long,
                                       title: String,
                                       description: String,
                                       introduction: String,
                                       metaUrl: String,
                                       coverPhotoUrl: Option[String],
                                       duration: Option[Int],
                                       status: String,
                                       lastUpdated: String,
                                       tags: Seq[String],
                                       supportedLanguages: Seq[String],
                                       isBasedOn: Option[Long])

