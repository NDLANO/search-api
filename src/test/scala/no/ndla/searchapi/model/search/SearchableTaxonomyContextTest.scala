/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.taxonomy.TaxonomyFilter
import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

class SearchableTaxonomyContextTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableLearningPath to json and deserializing back to object does not change content") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val originals = List(
      SearchableTaxonomyContext(
        id = "urn:resource:101",
        subjectId = "urn:subject:1",
        subject = SearchableLanguageValues(Seq(LanguageValue("nb", "Matte"))),
        path = "/subject:3/topic:1/topic:151/resource:101",
        breadcrumbs = SearchableLanguageList(Seq(
          LanguageValue("nb", Seq("Matte", "Østen for solen", "Vesten for månen"))
        )),
        contextType = LearningResourceType.Article.toString,
        filters = List(TaxonomyFilter(
          filterId = "urn:filter:1",
          name = SearchableLanguageValues(Seq(LanguageValue("nb", "VG1"))),
          relevance = SearchableLanguageValues(Seq(LanguageValue("nb", "Kjernestoff")))
        )
        ),
        resourceTypeIds = List("urn:resourcetype:subjectMaterial", "urn:resourcetype:academicArticle"),
        resourceTypes = SearchableLanguageList(Seq(LanguageValue("nb", Seq("Fagstoff", "Fagartikkel"))))
      )
    )

    val json = write(originals)
    val deserialized = read[List[SearchableTaxonomyContext]](json)

    deserialized should be(originals)
  }
}
