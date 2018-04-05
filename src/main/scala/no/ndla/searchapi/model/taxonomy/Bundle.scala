/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class Bundle(
    filters: List[Filter],
    relevances: List[Relevance],
    resourceFilterConnections: List[ResourceFilterConnection],
    resourceResourceTypeConnections: List[ResourceResourceTypeConnection],
    resourceTypes: List[ResourceType],
    resources: List[Resource],
    subjectTopicConnections: List[SubjectTopicConnection],
    subjects: List[Resource],
    topicFilterConnections: List[TopicFilterConnection],
    topicResourceConnections: List[TopicResourceConnection],
    topicSubtopicConnections: List[TopicSubtopicConnection],
    topics: List[Resource]
) {
  def getResourceTopics(resource: Resource): List[Resource] = {
    val tc = topicResourceConnections.filter(_.resourceId == resource.id)
    topics.filter(topic => tc.map(_.topicid).contains(topic.id))
  }

  def getObject(id: String): Option[Resource] = {
    if (id.contains(":resource:")) {
      resources.find(_.id == id)
    } else if (id.contains(":topic:")) {
      topics.find(_.id == id)
    } else if (id.contains(":subject:")) {
      subjects.find(_.id == id)
    } else {
      None
    }
  }
}
