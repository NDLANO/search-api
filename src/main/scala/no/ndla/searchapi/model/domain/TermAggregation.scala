package no.ndla.searchapi.model.domain

case class Bucket(value: String, count: Long)
case class TermAggregation(field: Seq[String],
                           sumOtherDocCount: Int,
                           docCountErrorUpperBound: Int,
                           buckets: Seq[Bucket])
