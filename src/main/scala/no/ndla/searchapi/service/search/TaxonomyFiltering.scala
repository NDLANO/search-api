/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.{BoolQuery, NestedQuery}
import no.ndla.searchapi.model.domain.article.LearningResourceType

trait TaxonomyFiltering {

  protected def relevanceFilter(relevanceIds: List[String],
                                subjectIds: List[String],
                                levels: List[String]): Option[BoolQuery] =
    if (relevanceIds.isEmpty) None
    else
      Some(
        boolQuery().should(
          relevanceIds.map(
            relevanceId =>
              nestedQuery(
                "contexts",
                boolQuery().must(
                  nestedQuery(
                    "contexts.filters",
                    boolQuery().must(
                      termQuery("contexts.filters.relevanceId", relevanceId),
                      boolQuery().must(levels.map(f => termQuery(s"contexts.filters.filterId", f)))
                    )
                  ),
                  boolQuery().should(subjectIds.map(sId => termQuery("contexts.subjectId", sId)))
                )
            )
          )
        ))

  protected def subjectFilter(subjects: List[String]): Option[NestedQuery] =
    if (subjects.isEmpty) None
    else
      Some(
        nestedQuery(path = "contexts",
                    boolQuery().should(
                      subjects.map(
                        subjectId => termQuery(s"contexts.subjectId", subjectId)
                      ))))

  protected def topicFilter(topics: List[String]): Option[NestedQuery] =
    if (topics.isEmpty) None
    else
      Some(
        nestedQuery("contexts",
                    boolQuery().should(
                      topics.map(
                        topicId => termQuery("contexts.parentTopicIds", topicId)
                      ))))

  protected def levelFilter(taxonomyFilters: List[String]): Option[NestedQuery] =
    if (taxonomyFilters.isEmpty) None
    else
      Some(
        nestedQuery("contexts.filters",
                    boolQuery().should(
                      taxonomyFilters.map(
                        filterId => termQuery("contexts.filters.filterId", filterId)
                      ))))

  protected def resourceTypeFilter(resourceTypes: List[String]): Option[NestedQuery] =
    if (resourceTypes.isEmpty) None
    else
      Some(
        nestedQuery("contexts.resourceTypes",
                    boolQuery().should(
                      resourceTypes.map(
                        resourceTypeId => termQuery("contexts.resourceTypes.id", resourceTypeId)
                      ))))

  protected def contextTypeFilter(contextTypes: List[LearningResourceType.Value]): Option[BoolQuery] =
    if (contextTypes.isEmpty) None
    else {
      val articleTypeQuery = contextTypes.map(ct => termQuery("articleType", ct.toString))

      val notArticleTypeQuery = if (contextTypes.contains(LearningResourceType.LearningPath)) {
        List(boolQuery().not(existsQuery("articleType")))
      } else {
        List.empty
      }

      val taxonomyContextQuery =
        contextTypes.map(ct => nestedQuery("contexts", termQuery("contexts.contextType", ct.toString)))

      Some(
        boolQuery().should(articleTypeQuery ++ taxonomyContextQuery ++ notArticleTypeQuery)
      )
    }

}
