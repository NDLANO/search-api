package no.ndla.searchapi.repository

import no.ndla.searchapi.SearchApiProperties.IndexBulkSize
import no.ndla.searchapi.model.domain.Content
import scalikejdbc._

import scala.math.ceil

trait Repository[T <: Content] {
  val connectionPoolName: Symbol

  def getByPage(pageSize: Int, offset: Int)(
      implicit session: DBSession = ReadOnlyNamedAutoSession(connectionPoolName)): Seq[T]
  protected def documentCount(implicit session: DBSession = ReadOnlyNamedAutoSession(connectionPoolName)): Long

  def pageCount(pageSize: Int)(implicit session: DBSession = ReadOnlyNamedAutoSession(connectionPoolName)): Int = {
    val dbCount = documentCount
    val pageSize = IndexBulkSize
    ceil(dbCount.toDouble / pageSize.toDouble).toInt
  }
}
