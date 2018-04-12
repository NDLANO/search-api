/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */


package no.ndla.searchapi.controller

import java.util.concurrent.{Executors, TimeUnit}

import javax.servlet.http.HttpServletRequest
import no.ndla.network.jwt.JWTExtractor
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.model.domain.ReindexResult
import no.ndla.searchapi.service.search.{ArticleIndexService, DraftIndexService, IndexService, LearningPathIndexService}
import org.apache.logging.log4j.ThreadContext
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait InternController {
  this: IndexService
    with ArticleIndexService
    with LearningPathIndexService
    with DraftIndexService =>
  val internController: InternController

  class InternController extends NdlaController {
    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(SearchApiProperties.SearchIndexes.size))

    private def resolveResultFutures(indexResults: List[Future[(String, Try[ReindexResult])]]): ActionResult = {

      val futureIndexed = Future.sequence(indexResults)
      val completedIndexed = Await.result(futureIndexed, Duration(10, TimeUnit.MINUTES))

      completedIndexed.collect{case (name, Failure(ex)) => (name, ex)} match {
        case Nil =>
          val successful = completedIndexed.collect{case (name, Success(r)) => (name, r)}

          val indexResults = successful.map({
            case (name: String, reindexResult: ReindexResult) =>
            s"${reindexResult.totalIndexed} $name in ${reindexResult.millisUsed} ms"
          }).mkString(", and ")
          val resultString = s"Completed indexing of $indexResults"

          logger.info(resultString)
          Ok(resultString)
        case failures =>

          val failedIndexResults = failures.map({
            case (name: String, failure: Throwable) =>
              logger.error(s"Failed to index $name: ${failure.getMessage}.", failure)
              s"$name: ${failure.getMessage}"
          }).mkString(", and ")

          InternalServerError(failedIndexResults)
      }
    }

    post("/index/draft") {
      val requestInfo = getRequestInfo
      val draftIndex = Future {
        requestInfo.setRequestInfo()
        ("drafts", draftIndexService.indexDocuments)
      }

      resolveResultFutures(List(draftIndex))
    }

    post("/index/article") {
      val requestInfo = getRequestInfo
      val articleIndex = Future {
        requestInfo.setRequestInfo()
        ("articles", articleIndexService.indexDocuments)
      }

      resolveResultFutures(List(articleIndex))
    }

    post("/index/learningpath") {
      val requestInfo = getRequestInfo
      val learningPathIndex = Future {
        requestInfo.setRequestInfo()
        ("learningpaths", learningPathIndexService.indexDocuments)
      }

      resolveResultFutures(List(learningPathIndex))
    }

    post("/index") {
      val requestInfo = getRequestInfo
      val indexes = List(
        Future {
          requestInfo.setRequestInfo()
          ("learningpaths", learningPathIndexService.indexDocuments)
        },
        Future {
          requestInfo.setRequestInfo()
          ("articles", articleIndexService.indexDocuments)
        },
        Future {
          requestInfo.setRequestInfo()
          ("drafts", draftIndexService.indexDocuments)
        }
      )

      resolveResultFutures(indexes)
    }

    /** Helper class to keep Thread specific request information in futures. */
    case class RequestInfo(CorrelationId: Option[String],
                           AuthHeader: Option[String],
                           UserId: Option[String],
                           Roles: List[String],
                           Name: Option[String],
                           ClientId: Option[String]) {
      def setRequestInfo(): Unit = {
        ThreadContext.put(SearchApiProperties.CorrelationIdKey, CorrelationId.getOrElse(""))
        CorrelationID.set(CorrelationId)
        AuthUser.setHeader(AuthHeader.getOrElse(""))
        AuthUser.setId(UserId)
        AuthUser.setRoles(Roles)
        AuthUser.setName(Name)
        AuthUser.setClientId(ClientId)
      }
    }

    private def getRequestInfo: RequestInfo = {
      val correlationId = CorrelationID.get
      val authHeader = AuthUser.getHeader
      val userId = AuthUser.get
      val roles = AuthUser.getRoles
      val name = AuthUser.getName
      val clientId = AuthUser.getClientId

      RequestInfo(correlationId, authHeader, userId, roles, name, clientId)
    }

  }
}
