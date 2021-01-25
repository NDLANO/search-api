package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Search suggestion options for the terms in the query")
case class TermValue(
    @(ApiModelProperty @field)(description = "Value that appeared in result") value: String,
    @(ApiModelProperty @field)(description = "Number of times the value appeared in result") count: Long
)

@ApiModel(description = "Search suggestion options for the terms in the query")
case class MultiSearchTermsAggregation(
    @(ApiModelProperty @field)(description = "The field the specific aggregation is matching") field: String,
    @(ApiModelProperty @field)(description = "Number of documents with values that didn't appear in the aggregation. (Will only happen if there are more than 50 different values)") sumOtherDocCount: Int,
    @(ApiModelProperty @field)(description = "The result is approximate, this gives an approximation of potential errors. (Specifics here: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html#search-aggregations-bucket-terms-aggregation-approximate-counts)")
    docCountErrorUpperBound: Int,
    @(ApiModelProperty @field)(description = "Values appearing in the field") values: Seq[TermValue]
)
// format: on
