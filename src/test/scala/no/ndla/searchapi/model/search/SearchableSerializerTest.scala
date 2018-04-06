/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.api.learningpath.{Author, Copyright, License}
import no.ndla.searchapi.model.domain.SearchableTaxonomyContext
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.learningpath.{LearningPathStatus, LearningPathVerificationStatus, StepType}
import no.ndla.searchapi.model.taxonomy.ContextFilter
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.json4s.native.Serialization.{read, write}
import org.json4s.Formats

class SearchableSerializerTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableArticle to json and deserializing back to object does not change content") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val titles = SearchableLanguageValues(Seq(
      LanguageValue("nb", "Christian Tut"),
      LanguageValue("en", "Christian Honk")))

    val contents = SearchableLanguageValues(Seq(
      LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
      LanguageValue("nb", "Jeg kjører rundt i tutut"),
      LanguageValue("en", "I'm in my mums car wroomwroom")))

    val visualElements = SearchableLanguageValues(Seq(
      LanguageValue("nn", "image"),
      LanguageValue("nb", "image")))

    val introductions = SearchableLanguageValues(Seq(
      LanguageValue("en", "Wroom wroom")
    ))

    val metaDescriptions = SearchableLanguageValues(Seq(
      LanguageValue("nb", "Mammas bil")
    ))

    val tags = SearchableLanguageList(Seq(
      LanguageValue("en", Seq("Mum", "Car", "Wroom"))
    ))

    val taxonomyContexts = List(
      SearchableTaxonomyContext(
        id = "urn:resource:101",
        subject = SearchableLanguageValues(Seq(LanguageValue("nb", "Matte"))),
        path = "/subject:3/topic:1/topic:151/resource:101",
        breadcrumbs = SearchableLanguageList(Seq(
          LanguageValue("nb", Seq("Matte", "Østen for solen", "Vesten for månen"))
        )),
        contextType = LearningResourceType.Standard.toString,
        filters = List(ContextFilter(
          name = SearchableLanguageValues(Seq(LanguageValue("nb", "VG1"))),
          relevance = SearchableLanguageValues(Seq(LanguageValue("nb", "Kjernestoff")))
        )
        ),
        resourceTypes = SearchableLanguageList(Seq(LanguageValue("nb", Seq("Fagstoff", "Fagartikkel"))))
      )
    )

    val original = SearchableArticle(
      id = 100,
      title = titles,
      content = contents,
      visualElement = visualElements,
      introduction = introductions,
      metaDescription = metaDescriptions,
      tags = tags,
      lastUpdated = TestData.today,
      license = "by-sa",
      authors = List("Jonas", "Papi"),
      articleType = LearningResourceType.Standard.toString,
      metaImageId = Some("1"),
      defaultTitle = Some("Christian Tut"),
      supportedLanguages = List("en", "nb", "nn"),
      contexts = taxonomyContexts
    )
    val json = write(original)
    val deserialized = read[SearchableArticle](json)

    deserialized should be(original)
  }

  test("That serializing a SearchableLearningPath to json and deserializing back to object does not change content") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val titles = SearchableLanguageValues(Seq(
      LanguageValue("nb", "Christian Tut"),
      LanguageValue("en", "Christian Honk")))

    val descriptions = SearchableLanguageValues(Seq(
      LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
      LanguageValue("nb", "Jeg kjører rundt i tutut"),
      LanguageValue("en", "I'm in my mums car wroomwroom")))

    val tags = SearchableLanguageList(Seq(
      LanguageValue("en", Seq("Mum", "Car", "Wroom"))
    ))

    val taxonomyContexts = List(
      SearchableTaxonomyContext(
        id = "urn:resource:101",
        subject = SearchableLanguageValues(Seq(LanguageValue("nb", "Matte"))),
        path = "/subject:3/topic:1/topic:151/resource:101",
        breadcrumbs = SearchableLanguageList(Seq(
          LanguageValue("nb", Seq("Matte", "Østen for solen", "Vesten for månen"))
        )),
        contextType = LearningResourceType.Standard.toString,
        filters = List(ContextFilter(
          name = SearchableLanguageValues(Seq(LanguageValue("nb", "VG1"))),
          relevance = SearchableLanguageValues(Seq(LanguageValue("nb", "Kjernestoff")))
        )
        ),
        resourceTypes = SearchableLanguageList(Seq(LanguageValue("nb", Seq("Fagstoff", "Fagartikkel"))))
      )
    )

    val learningsteps = List(
      SearchableLearningStep(
        stepType = StepType.INTRODUCTION.toString,
        title = SearchableLanguageValues(Seq(LanguageValue("nb", "Introsteg"))),
        description = SearchableLanguageValues(Seq(LanguageValue("nb", "IntroDesc")))
      ),
      SearchableLearningStep(
        stepType = StepType.SUMMARY.toString,
        title = SearchableLanguageValues(Seq(LanguageValue("nb", "Summary a gitt"))),
        description = SearchableLanguageValues(Seq(LanguageValue("nb", "Summariereire"), LanguageValue("en", "Summingz")))
      ),
      SearchableLearningStep(
        stepType = StepType.TEXT.toString,
        title = SearchableLanguageValues(Seq(LanguageValue("en", "TEXTITexTy"))),
        description = SearchableLanguageValues(Seq(LanguageValue("en", "TextarUøø")))
      )
    )

    val original = SearchableLearningPath(
      id = 101,
      title = titles,
      description = descriptions,
      coverPhotoId = Some("10"),
      duration = Some(10),
      status = LearningPathStatus.PUBLISHED.toString,
      verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA.toString,
      lastUpdated = TestData.today,
      defaultTitle = Some("Christian Tut"),
      tags = tags,
      learningsteps = learningsteps,
      license = Copyright(License("by-sa", Some("bysasaa"), None), Seq(Author("Supplier", "Jonas"), Author("Originator", "Kakemonsteret"))),
      isBasedOn = Some(1001),
      contexts = taxonomyContexts
    )

    val json = write(original)
    val deserialized = read[SearchableLearningPath](json)

    deserialized should be(original)
  }
}
