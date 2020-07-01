/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class MetaImage(imageId: String, altText: String, language: String) extends LanguageField
