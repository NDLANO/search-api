/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

abstract class TaxonomyElement(id: String,
                               name: String,
                               contentUri: Option[String],
                               path: String,
                               metadata: Option[Metadata]) {
  def getId(): String = id
  def getName(): String = name
}

case class TaxSubject(id: String, name: String, contentUri: Option[String], path: String, metadata: Option[Metadata])
    extends TaxonomyElement(id, name, contentUri, path, metadata)
case class Resource(id: String, name: String, contentUri: Option[String], path: String, metadata: Option[Metadata])
    extends TaxonomyElement(id, name, contentUri, path, metadata)
case class Topic(id: String, name: String, contentUri: Option[String], path: String, metadata: Option[Metadata])
    extends TaxonomyElement(id, name, contentUri, path, metadata)
