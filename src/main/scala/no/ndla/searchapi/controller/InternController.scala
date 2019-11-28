/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.controller

import java.util.concurrent.{Executors, TimeUnit}

import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.TaxonomyApiClient
import no.ndla.searchapi.model.api.InvalidIndexBodyException
import no.ndla.searchapi.model.domain.article.Article
import no.ndla.searchapi.model.domain.draft.Draft
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.searchapi.model.domain.{ReindexResult, RequestInfo}
import no.ndla.searchapi.service.search.{ArticleIndexService, DraftIndexService, IndexService, LearningPathIndexService}
import org.scalatra._

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait InternController {
  this: IndexService
    with ArticleIndexService
    with LearningPathIndexService
    with DraftIndexService
    with TaxonomyApiClient =>
  val internController: InternController

  class InternController extends NdlaController {
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(SearchApiProperties.SearchIndexes.size))

    private def resolveResultFutures(indexResults: List[Future[(String, Try[ReindexResult])]]): ActionResult = {

      val futureIndexed = Future.sequence(indexResults)
      val completedIndexed = Await.result(futureIndexed, Duration(60, TimeUnit.MINUTES))

      completedIndexed.collect { case (name, Failure(ex)) => (name, ex) } match {
        case Nil =>
          val successful = completedIndexed.collect { case (name, Success(r)) => (name, r) }

          val indexResults = successful
            .map({
              case (name: String, reindexResult: ReindexResult) =>
                s"${reindexResult.totalIndexed} $name in ${reindexResult.millisUsed} ms"
            })
            .mkString(", and ")
          val resultString = s"Completed indexing of $indexResults"

          logger.info(resultString)
          Ok(resultString)
        case failures =>
          val failedIndexResults = failures
            .map({
              case (name: String, failure: Throwable) =>
                logger.error(s"Failed to index $name: ${failure.getMessage}.", failure)
                s"$name: ${failure.getMessage}"
            })
            .mkString(", and ")

          InternalServerError(failedIndexResults)
      }
    }

    delete("/:type/:id") {
      val indexType = params("type")
      val documentId = long("id")

      indexType match {
        case articleIndexService.documentType      => articleIndexService.deleteDocument(documentId)
        case draftIndexService.documentType        => draftIndexService.deleteDocument(documentId)
        case learningPathIndexService.documentType => learningPathIndexService.deleteDocument(documentId)
      }
    }

    private def parseBody[T](body: String)(implicit mf: Manifest[T]): Try[T] = {
      Try(parse(body).camelizeKeys.extract[T])
        .recoverWith { case _ => Failure(InvalidIndexBodyException()) }
    }

    post("/:type/") {
      val indexType = params("type")

      val indexedTry = indexType match {
        case articleIndexService.documentType =>
          parseBody[Article](request.body).flatMap(a => articleIndexService.indexDocument(a))
        case draftIndexService.documentType =>
          parseBody[Draft](request.body).flatMap(d => draftIndexService.indexDocument(d))
        case learningPathIndexService.documentType =>
          parseBody[LearningPath](request.body).flatMap(l => learningPathIndexService.indexDocument(l))
      }

      indexedTry match {
        case Success(doc) => Created(doc)
        case Failure(ex) =>
          logger.error("Could not index document...", ex)
          errorHandler(ex)
      }
    }

    post("/index/draft") {
      val requestInfo = RequestInfo()
      val draftIndex = Future {
        requestInfo.setRequestInfo()
        ("drafts", draftIndexService.indexDocuments())
      }

      resolveResultFutures(List(draftIndex))
    }

    post("/index/article") {
      val requestInfo = RequestInfo()
      val articleIndex = Future {
        requestInfo.setRequestInfo()
        ("articles", articleIndexService.indexDocuments())
      }

      resolveResultFutures(List(articleIndex))
    }

    post("/index/learningpath") {
      val requestInfo = RequestInfo()
      val learningPathIndex = Future {
        requestInfo.setRequestInfo()
        ("learningpaths", learningPathIndexService.indexDocuments())
      }

      resolveResultFutures(List(learningPathIndex))
    }

    post("/index") {
      val runInBackground = booleanOrDefault("run-in-background", default = false)
      taxonomyApiClient.getTaxonomyBundle match {
        case Success(bundle) =>
          val requestInfo = RequestInfo()

          val indexes = List(
            Future {
              requestInfo.setRequestInfo()
              ("learningpaths", learningPathIndexService.indexDocuments(bundle))
            },
            Future {
              requestInfo.setRequestInfo()
              ("articles", articleIndexService.indexDocuments(bundle))
            },
            Future {
              requestInfo.setRequestInfo()
              ("drafts", draftIndexService.indexDocuments(bundle))
            }
          )
          if (runInBackground) {
            Accepted("Starting indexing process...")
          } else {
            resolveResultFutures(indexes)
          }
        case Failure(ex) => errorHandler(ex)
      }
    }

  }
}
