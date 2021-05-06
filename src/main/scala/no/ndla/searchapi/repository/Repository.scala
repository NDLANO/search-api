package no.ndla.searchapi.repository

import no.ndla.searchapi.model.domain.Content
import scalikejdbc._

trait Repository[T <: Content] {
  val connectionPoolName: Symbol

  def getByPage(pageSize: Int, offset: Int)(
      implicit session: DBSession = ReadOnlyNamedAutoSession(connectionPoolName)): Seq[T]
  protected def documentCount(implicit session: DBSession = ReadOnlyNamedAutoSession(connectionPoolName)): Long
  def pageCount(pageSize: Int)(implicit session: DBSession = ReadOnlyNamedAutoSession(connectionPoolName)): Int
}
