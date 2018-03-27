/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */


package no.ndla.searchapi.controller

import java.util.concurrent.{Executors, TimeUnit}

import no.ndla.network.{ApplicationUrl, AuthUser}
import no.ndla.searchapi.model.domain.ReindexResult
import no.ndla.searchapi.service.search.{ArticleIndexService, IndexService, LearningPathIndexService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait InternController {
  this: IndexService
    with ArticleIndexService
    with LearningPathIndexService =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats
    implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)

    private def resolveResultFuture(indexResults: Future[(Try[ReindexResult], Try[ReindexResult])]): ActionResult = {
      Await.result(indexResults, Duration(10, TimeUnit.MINUTES)) match {
        case (Success(articleResult), Success(learningPathResult)) =>
          val arIndexTime = math.max(articleResult.millisUsed, 0)
          val lpIndexTime = math.max(learningPathResult.millisUsed, 0)

          val result = s"Completed indexing of ${articleResult.totalIndexed} articles in $arIndexTime ms, and ${learningPathResult.totalIndexed} learningpaths in $lpIndexTime ms."
          logger.info(result)
          Ok(result)
        case (Failure(articleFail), _) =>
          logger.warn(s"Failed to index articles: ${articleFail.getMessage}", articleFail)
          InternalServerError(articleFail.getMessage)
        case (_, Failure(learningPathFail)) =>
          logger.warn(s"Failed to index learningpaths: ${learningPathFail.getMessage}", learningPathFail)
          InternalServerError(learningPathFail.getMessage)
      }
    }

    post("/index/article") {
      val indexResults = for {
        articleIndex <- Future {
          AuthUser.set(request)
          articleIndexService.indexDocuments
        }
      } yield (articleIndex, Success(ReindexResult(0,0)))

      resolveResultFuture(indexResults)
    }

    post("/index/learningpath") {
      val indexResults = for {
        learningPathIndex <- Future {
          AuthUser.set(request)
          learningPathIndexService.indexDocuments
        }
      } yield (Success(ReindexResult(0,0)), learningPathIndex)

      resolveResultFuture(indexResults)
    }

    post("/index") {
      val indexResults = for {
        articleIndex <- Future {
          AuthUser.set(request)
          articleIndexService.indexDocuments
        }
        learningPathIndex <- Future {
          AuthUser.set(request)
          learningPathIndexService.indexDocuments
        }
      } yield (articleIndex, learningPathIndex)

      resolveResultFuture(indexResults)
    }
  }
}
