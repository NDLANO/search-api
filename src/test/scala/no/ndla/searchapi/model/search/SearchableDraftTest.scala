/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.domain.article.{ArticleMetaImage, LearningResourceType}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

class SearchableDraftTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableDraft to json and deserializing back to object does not change content") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val titles =
      SearchableLanguageValues(Seq(LanguageValue("nb", "Christian Tut"), LanguageValue("en", "Christian Honk")))

    val contents = SearchableLanguageValues(
      Seq(
        LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
        LanguageValue("nb", "Jeg kjører rundt i tutut"),
        LanguageValue("en", "I'm in my mums car wroomwroom")
      ))

    val visualElements = SearchableLanguageValues(Seq(LanguageValue("nn", "image"), LanguageValue("nb", "image")))

    val introductions = SearchableLanguageValues(
      Seq(
        LanguageValue("en", "Wroom wroom")
      ))

    val metaDescriptions = SearchableLanguageValues(
      Seq(
        LanguageValue("nb", "Mammas bil")
      ))

    val tags = SearchableLanguageList(
      Seq(
        LanguageValue("en", Seq("Mum", "Car", "Wroom"))
      ))

    val metaImages = List(ArticleMetaImage("1", "norAlt", "nb"), ArticleMetaImage("2", "enAlt", "en"))

    val original = SearchableDraft(
      id = 100,
      draftStatus = List(ArticleStatus.DRAFT.toString, ArticleStatus.PROPOSAL.toString),
      title = titles,
      content = contents,
      visualElement = visualElements,
      introduction = introductions,
      metaDescription = metaDescriptions,
      tags = tags,
      lastUpdated = TestData.today,
      license = Some("by-sa"),
      authors = List("Jonas", "Papi"),
      articleType = LearningResourceType.Article.toString,
      metaImage = metaImages,
      defaultTitle = Some("Christian Tut"),
      supportedLanguages = List("en", "nb", "nn"),
      notes = List("Note1", "note2"),
      contexts = searchableTaxonomyContexts,
      users = List("ndalId54321", "ndalId12345"),
      previousVersionsNotes = List("OldNote")
    )
    val json = write(original)
    val deserialized = read[SearchableDraft](json)

    deserialized should be(original)
  }

}
