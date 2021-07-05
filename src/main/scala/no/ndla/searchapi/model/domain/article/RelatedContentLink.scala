package no.ndla.searchapi.model.domain.article

case class RelatedContentLink(title: String, url: String)

object RelatedContentLink {
  type RelatedContent = Either[RelatedContentLink, Long]
}
