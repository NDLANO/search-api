/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.model.domain.article._
import no.ndla.network.ApplicationUrl
import no.ndla.searchapi.integration.DraftApiClient
import no.ndla.searchapi.model.domain.Language
import no.ndla.searchapi.model.search._
import org.jsoup.Jsoup

trait SearchConverterService {
  this: DraftApiClient =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def withAgreementCopyright(article: Article): Article = {
      val agreementCopyright = article.copyright.agreementId.flatMap(aid =>
        draftApiClient.getAgreementCopyright(aid)
      ).getOrElse(article.copyright)

      article.copy(copyright = article.copyright.copy(
        license = agreementCopyright.license,
        creators = agreementCopyright.creators,
        rightsholders = agreementCopyright.rightsholders,
        validFrom = agreementCopyright.validFrom,
        validTo = agreementCopyright.validTo
      ))
    }

    def asSearchableArticle(ai: Article): SearchableArticle = {
      val articleWithAgreement = withAgreementCopyright(ai)

      val defaultTitle = articleWithAgreement.title.sortBy(title => {
        val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
        languagePriority.indexOf(title.language)
      }).lastOption

      SearchableArticle(
        id = articleWithAgreement.id.get,
        title = SearchableLanguageValues(articleWithAgreement.title.map(title => LanguageValue(title.language, title.title))),
        visualElement = SearchableLanguageValues(articleWithAgreement.visualElement.map(visual => LanguageValue(visual.language, visual.resource))),
        introduction = SearchableLanguageValues(articleWithAgreement.introduction.map(intro => LanguageValue(intro.language, intro.introduction))),
        metaDescription = SearchableLanguageValues(articleWithAgreement.metaDescription.map(meta => LanguageValue(meta.language, meta.content))),
        content = SearchableLanguageValues(articleWithAgreement.content.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
        tags = SearchableLanguageList(articleWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = articleWithAgreement.updated,
        license = articleWithAgreement.copyright.license,
        authors = articleWithAgreement.copyright.creators.map(_.name) ++ articleWithAgreement.copyright.processors.map(_.name) ++ articleWithAgreement.copyright.rightsholders.map(_.name),
        articleType = articleWithAgreement.articleType,
        defaultTitle = defaultTitle.map(t => t.title)
      )
    }

    def asArticleSummary(searchableArticle: SearchableArticle): ArticleSummary = {
      ArticleSummary(
        id = searchableArticle.id,
        title = searchableArticle.title.languageValues.map(lv => ArticleTitle(lv.value, lv.lang)),
        visualElement = searchableArticle.visualElement.languageValues.map(lv => VisualElement(lv.value, lv.lang)),
        introduction = searchableArticle.introduction.languageValues.map(lv => ArticleIntroduction(lv.value, lv.lang)),
        url = createUrlToArticle(searchableArticle.id),
        license = searchableArticle.license)
    }

    def createUrlToArticle(id: Long): String = {
      s"${ApplicationUrl.get}$id"
    }

    def asSearchableConcept(c: Concept): SearchableConcept = {

      val defaultTitle = c.title.sortBy(title => {
        val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
        languagePriority.indexOf(title.language)
      }).lastOption

      SearchableConcept(
        c.id.get,
        SearchableLanguageValues(c.title.map(title => LanguageValue(title.language, title.title))),
        SearchableLanguageValues(c.content.map(content => LanguageValue(content.language, content.content))),
        defaultTitle.map(t => t.title)
      )
    }

  }
}
