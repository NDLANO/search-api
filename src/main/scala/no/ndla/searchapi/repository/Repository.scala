package no.ndla.searchapi.repository

import no.ndla.searchapi.model.domain.Content
import scalikejdbc._

trait Repository[T <: Content] {

  def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession = ReadOnlyAutoSession): Seq[T]
  protected def documentCount(implicit session: DBSession = ReadOnlyAutoSession): Long
  def pageCount(pageSize: Int): Int
}
