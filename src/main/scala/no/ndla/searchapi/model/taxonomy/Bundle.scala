/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

import no.ndla.searchapi.integration._

case class Bundle(
                   filters: Seq[Filter],
                   relevances: Seq[Relevance],
                   resourceFilterConnections: Seq[ResourceFilterConnection],
                   resourceResourceTypeConnections: Seq[
      ResourceResourceTypeConnection],
                   resourceTypes: Seq[ResourceType],
                   resources: Seq[Resource],
                   subjectTopicConnections: Seq[SubjectTopicConnection],
                   subjects: Seq[Resource],
                   topicFilterConnections: Seq[TopicFilterConnection],
                   topicResourceConnections: Seq[TopicResourceConnection],
                   topicSubtopicConnections: Seq[TopicSubtopicConnection],
                   topics: Seq[Resource]
)
