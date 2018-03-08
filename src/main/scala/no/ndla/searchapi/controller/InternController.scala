/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */


package no.ndla.searchapi.controller

import java.util.concurrent.{Executors, TimeUnit}

import no.ndla.searchapi.service.search.{ArticleIndexService, IndexService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{BadRequest, InternalServerError, NotFound, Ok}

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

trait InternController {
  this: IndexService with ArticleIndexService =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val indexResults = for {
        articleIndex <- Future { articleIndexService.indexDocuments }
        //TODO: learningpathIndex <- Future { learningpathIndexService.indexDocuments }
      } yield articleIndex //TODO: Yield the others


      Await.result(indexResults, Duration(10, TimeUnit.MINUTES)) match {
        case (Success(articleResult)) => //TODO: match others also
          val indexTime = math.max(articleResult.millisUsed, 0)
          val result = s"Completed indexing of ${articleResult.totalIndexed} articles in $indexTime ms." // TODO: update this
          logger.info(result)
          Ok(result)
        case (Failure(articleFail)) =>
          logger.warn(articleFail.getMessage, articleFail)
          InternalServerError(articleFail.getMessage)
      }
    }
  }
}
