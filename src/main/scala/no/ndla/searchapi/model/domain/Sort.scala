/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

object Sort  extends Enumeration {
  val ByRelevanceDesc = Value("-relevance")

  def valueOf(s:String): Option[Sort.Value] = {
    Sort.values.find(_.toString == s)
  }

  def valueOf(s:Option[String]): Option[Sort.Value] = {
    s match {
      case None => None
      case Some(s) => valueOf(s)
    }
  }
}

