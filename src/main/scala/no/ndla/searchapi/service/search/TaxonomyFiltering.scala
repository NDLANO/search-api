/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search
import no.ndla.mapping.ISO639
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.BoolQuery
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
              nestedQuery("contexts").query(
                boolQuery().must(
                  nestedQuery("contexts.filters").query(boolQuery().must(
                    termQuery("contexts.filters.relevanceId", relevanceId),
                    boolQuery().should(levels.map(f =>
                      boolQuery().should(ISO639.languagePriority.map(l =>
                        termQuery(s"contexts.filters.name.$l.raw", f)))))
                  )),
                  boolQuery().should(subjectIds.map(sId => termQuery("contexts.subjectId", sId)))
                )
            )
          )
        ))

  protected def subjectFilter(subjects: List[String]): Option[BoolQuery] =
    if (subjects.isEmpty) None
    else
      Some(
        boolQuery().should(
          subjects.map(
            subjectId =>
              nestedQuery("contexts").query(
                termQuery(s"contexts.subjectId", subjectId)
            ))
        )
      )

  protected def topicFilter(topics: List[String]): Option[BoolQuery] =
    if (topics.isEmpty) None
    else
      Some(
        boolQuery().should(
          topics.map(
            topicId =>
              nestedQuery("contexts").query(
                termQuery(s"contexts.parentTopicIds", topicId)
            ))
        )
      )

  protected def levelFilter(taxonomyFilters: List[String]): Option[BoolQuery] =
    if (taxonomyFilters.isEmpty) None
    else
      Some(
        boolQuery().should(
          taxonomyFilters.map(
            filterName =>
              nestedQuery("contexts.filters").query(
                boolQuery().should(
                  ISO639.languagePriority.map(l => termQuery(s"contexts.filters.name.$l.raw", filterName))
                )
            ))
        )
      )

  protected def resourceTypeFilter(resourceTypes: List[String]): Option[BoolQuery] =
    if (resourceTypes.isEmpty) None
    else
      Some(
        boolQuery().must(
          resourceTypes.map(
            resourceTypeId =>
              nestedQuery("contexts.resourceTypes").query(
                termQuery(s"contexts.resourceTypes.id", resourceTypeId)
            ))
        )
      )

  protected def contextTypeFilter(contextTypes: List[LearningResourceType.Value]): Option[BoolQuery] =
    if (contextTypes.isEmpty) None
    else
      Some(
        boolQuery().should(
          contextTypes.map(
            ct =>
              nestedQuery("contexts").query(
                termQuery("contexts.contextType", ct.toString)
            ))
        )
      )

}
