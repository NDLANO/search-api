/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

import no.ndla.searchapi.integration._

case class TaxonomyBundle(
    filters: Seq[TaxonomyFilter],
    relevances: Seq[TaxonomyRelevance],
    resourceFilterConnections: Seq[TaxonomyResourceFilterConnection],
    resourceResourceTypeConnections: Seq[
      TaxonomyResourceResourceTypeConnection],
    resourceTypes: Seq[TaxonomyResourceType],
    resources: Seq[TaxonomyResource],
    subjectTopicConnections: Seq[TaxonomySubjectTopicConnection],
    subjects: Seq[TaxonomyResource],
    topicFilterConnections: Seq[TaxonomyTopicFilterConnection],
    topicResourceConnections: Seq[TaxonomyTopicResourceConnection],
    topicSubtopicConnections: Seq[TaxonomyTopicSubtopicConnection],
    topics: Seq[TaxonomyResource]
)
