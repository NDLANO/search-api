package no.ndla.searchapi.model.domain

import no.ndla.searchapi.SearchApiProperties.DatabaseDetails.DatabaseDetails
import org.json4s.Formats
import scalikejdbc._

trait NDLASQLSupport[T] extends SQLSyntaxSupport[T] {
  val jsonEncoder: Formats
  val repositorySerializer: Formats
  val dbInfo: DatabaseDetails

  override lazy val schemaName: Option[String] = Some(dbInfo.schema)
  override lazy val connectionPoolName: Symbol = dbInfo.connectionPoolName

  def fromResultSet(sp: SyntaxProvider[T])(rs: WrappedResultSet): Option[T] = fromResultSet(sp.resultName)(rs)
  def fromResultSet(rn: ResultName[T])(rs: WrappedResultSet): Option[T]
}
