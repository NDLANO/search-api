/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import com.sksamuel.elastic4s.analyzers._
import no.ndla.language.model.LanguageTag
import no.ndla.searchapi.SearchApiProperties.DefaultLanguage

object Language {
  val UnknownLanguage: LanguageTag = LanguageTag("und")
  val NoLanguage = ""
  val AllLanguages = "*"
  val Nynorsk = "nynorsk"

  // Must be included in search index settings
  val nynorskLanguageAnalyzer: CustomAnalyzerDefinition = CustomAnalyzerDefinition(
    name = Nynorsk,
    tokenizer = StandardTokenizer,
    filters = Seq(LowercaseTokenFilter,
                  StopTokenFilter("norwegian_stop"),
                  StemmerTokenFilter("nynorsk_stemmer", lang = "light_nynorsk"))
  )

  val languageAnalyzers = Seq(
    LanguageAnalyzer(LanguageTag("nb"), NorwegianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("nn"), CustomAnalyzer(Nynorsk)),
    LanguageAnalyzer(LanguageTag("sma"), StandardAnalyzer), // Southern sami
    LanguageAnalyzer(LanguageTag("se"), StandardAnalyzer), // Northern Sami
    LanguageAnalyzer(LanguageTag("en"), EnglishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ar"), ArabicLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("hy"), ArmenianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("eu"), BasqueLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("pt-br"), BrazilianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("bg"), BulgarianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ca"), CatalanLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ja"), CjkLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ko"), CjkLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("zh"), CjkLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("cs"), CzechLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("da"), DanishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("nl"), DutchLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("fi"), FinnishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("fr"), FrenchLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("gl"), GalicianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("de"), GermanLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("el"), GreekLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("hi"), HindiLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("hu"), HungarianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("id"), IndonesianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ga"), IrishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("it"), ItalianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("lt"), LithuanianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("lv"), LatvianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("fa"), PersianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("pt"), PortugueseLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ro"), RomanianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ru"), RussianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("srb"), SoraniLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("es"), SpanishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("sv"), SwedishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("th"), ThaiLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("tr"), TurkishLanguageAnalyzer),
    LanguageAnalyzer(UnknownLanguage, StandardAnalyzer)
  )

  def findByLanguageOrBestEffort[P <: LanguageField](sequence: Seq[P], language: String): Option[P] = {
    sequence
      .find(_.language == language)
      .orElse(
        sequence
          .sortBy(lf => languageAnalyzers.map(la => la.languageTag.toString).reverse.indexOf(lf.language))
          .lastOption)
  }

  def languageOrUnknown(language: Option[String]): LanguageTag = {
    language.filter(_.nonEmpty) match {
      case Some(x) if x == "unknown" => UnknownLanguage
      case Some(x)                   => LanguageTag(x)
      case None                      => UnknownLanguage
    }
  }

  def getSupportedLanguages(sequences: Seq[LanguageField]*): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      languageAnalyzers.map(la => la.languageTag.toString).indexOf(lang)
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

case class LanguageAnalyzer(languageTag: LanguageTag, analyzer: Analyzer)
