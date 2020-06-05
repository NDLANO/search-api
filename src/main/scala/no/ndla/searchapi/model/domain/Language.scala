/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import com.sksamuel.elastic4s.analysis.{CustomAnalyzer, LanguageAnalyzers, StemmerTokenFilter}
import no.ndla.mapping.ISO639

object Language {
  val DefaultLanguage = "nb"
  val UnknownLanguage = "unknown"
  val NoLanguage = ""
  val AllLanguages = "all"
  val Nynorsk = "nynorsk"

  // Must be included in search index settings
  val nynorskStemmer: StemmerTokenFilter = StemmerTokenFilter("nynorsk_stemmer", lang = "light_nynorsk")

  val nynorskLanguageAnalyzer: CustomAnalyzer = CustomAnalyzer(
    name = Nynorsk,
    tokenizer = "standard",
    tokenFilters = List("lowercase", "nynorsk_stemmer")
  )

  val languageAnalyzers = List(
    LanguageAnalyzer(DefaultLanguage, LanguageAnalyzers.norwegian),
    LanguageAnalyzer("nn", Nynorsk),
    LanguageAnalyzer("en", LanguageAnalyzers.english),
    LanguageAnalyzer("fr", LanguageAnalyzers.french),
    LanguageAnalyzer("de", LanguageAnalyzers.german),
    LanguageAnalyzer("es", LanguageAnalyzers.spanish),
    LanguageAnalyzer("se", "standard"), // SAMI
    LanguageAnalyzer("sma", "standard"), // SAMI
    LanguageAnalyzer("zh", LanguageAnalyzers.cjk),
    LanguageAnalyzer(UnknownLanguage, "standard")
  )

  val supportedLanguages: Seq[String] = languageAnalyzers.map(_.lang)

  def findByLanguageOrBestEffort[P <: LanguageField](sequence: Seq[P], language: String): Option[P] = {
    sequence
      .find(_.language == language)
      .orElse(sequence.sortBy(lf => ISO639.languagePriority.reverse.indexOf(lf.language)).lastOption)
  }

  def languageOrUnknown(language: Option[String]): String = {
    language.filter(_.nonEmpty) match {
      case Some(x) => x
      case None    => UnknownLanguage
    }
  }

  def getSupportedLanguages(sequences: Seq[LanguageField]*): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      ISO639.languagePriority.indexOf(lang)
    }
  }

  def getSearchLanguage(languageParam: String, supportedLanguages: Seq[String]): String = {
    val l = if (languageParam == AllLanguages) DefaultLanguage else languageParam
    if (supportedLanguages.contains(l))
      l
    else
      supportedLanguages.head
  }

  def findByLanguage[T](sequence: Seq[LanguageField], lang: String): Option[LanguageField] = {
    sequence.find(_.language == lang)
  }
}

case class LanguageAnalyzer(lang: String, analyzer: String)
