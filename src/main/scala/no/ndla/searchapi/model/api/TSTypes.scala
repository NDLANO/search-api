package no.ndla.searchapi.model.api

import com.scalatsi._

/**
  * The `scala-tsi` plugin is not always able to derive the types that are used in `Seq` or other generic types.
  * Therefore we need to explicitly load the case classes here.
  * This is only necessary if the `sbt generateTypescript` script fails.
  */
object TSTypes {
  implicit val articleResult: TSIType[ArticleResult] = TSType.fromCaseClass[ArticleResult]
  implicit val audioResult: TSIType[AudioResult] = TSType.fromCaseClass[AudioResult]
  implicit val imageResult: TSIType[ImageResult] = TSType.fromCaseClass[ImageResult]
  implicit val learningpathResult: TSIType[LearningpathResult] = TSType.fromCaseClass[LearningpathResult]
  implicit val taxonomyResourceType: TSIType[TaxonomyResourceType] = TSType.fromCaseClass[TaxonomyResourceType]
  implicit val taxonomyContextFilter: TSIType[TaxonomyContextFilter] = TSType.fromCaseClass[TaxonomyContextFilter]
  implicit val validationMessage: TSIType[ValidationMessage] = TSType.fromCaseClass[ValidationMessage]
  implicit val apiTaxonomyContext: TSIType[ApiTaxonomyContext] = TSType.fromCaseClass[ApiTaxonomyContext]
  implicit val highlightedField: TSIType[HighlightedField] = TSType.fromCaseClass[HighlightedField]
  implicit val suggestOption: TSIType[SuggestOption] = TSType.fromCaseClass[SuggestOption]
  implicit val searchSuggestion: TSIType[SearchSuggestion] = TSType.fromCaseClass[SearchSuggestion]
  implicit val multiSearchSummary: TSIType[MultiSearchSummary] = TSType.fromCaseClass[MultiSearchSummary]
  implicit val multiSearchSuggestion: TSIType[MultiSearchSuggestion] = TSType.fromCaseClass[MultiSearchSuggestion]
  implicit val termValue: TSIType[TermValue] = TSType.fromCaseClass[TermValue]
  implicit val multiSearchTermsAggregation: TSIType[MultiSearchTermsAggregation] =
    TSType.fromCaseClass[MultiSearchTermsAggregation]
  implicit val multiSearchResult: TSIType[MultiSearchResult] = TSType.fromCaseClass[MultiSearchResult]
}
