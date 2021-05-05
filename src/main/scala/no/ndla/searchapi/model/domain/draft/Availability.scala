package no.ndla.searchapi.model.domain.draft

object Availability extends Enumeration {
  val everyone, student, teacher = Value

  def valueOf(s: String): Option[Availability.Value] = {
    Availability.values.find(_.toString == s)
  }

  def valueOf(s: Option[String]): Option[Availability.Value] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }

}
