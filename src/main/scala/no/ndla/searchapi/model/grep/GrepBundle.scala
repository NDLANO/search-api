package no.ndla.searchapi.model.grep

case class GrepBundle(
    kjerneelementer: List[GrepElement],
    kompetansemaal: List[GrepElement],
    tverrfagligeTemaer: List[GrepElement],
)
