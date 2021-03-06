/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class SubjectTopicConnection(subjectid: String, topicid: String, id: String, primary: Boolean, rank: Int)
