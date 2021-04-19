/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import java.util
import com.typesafe.scalalogging.LazyLogging

import javax.servlet.DispatcherType
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import no.ndla.searchapi.model.domain.{Content, ReindexResult, RequestInfo}
import no.ndla.searchapi.service.search.IndexService
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import java.util.concurrent.Executors
import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.io.Source
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

object JettyLauncher extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    val envMap = System.getenv()
    envMap.asScala.foreach { case (k, v) => System.setProperty(k, v) }

    if (SearchApiProperties.booleanOrFalse("STANDALONE_INDEXING_ENABLED")) {
      doStandaloneIndexing()
    } else {
      val server = startServer(SearchApiProperties.ApplicationPort)
      server.join()
    }
  }

  def startServer(port: Int): Server = {
    val startMillis = System.currentTimeMillis()

    val context = new ServletContextHandler()
    context setContextPath "/"
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    context.addServlet(classOf[ReportServlet], "/monitoring")
    context.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, SearchApiProperties.ApplicationName)
    SearchApiProperties.Environment match {
      case "local" => None
      case _ =>
        monitoringFilter.setInitParameter(Parameter.CLOUDWATCH_NAMESPACE.getCode,
                                          "NDLA/APP".replace("APP", SearchApiProperties.ApplicationName))
    }
    context.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    val server = new Server(port)
    server.setHandler(context)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server
  }

  private def doStandaloneIndexing() = {
    val bundles = for {
      taxonomyBundle <- ComponentRegistry.taxonomyApiClient.getTaxonomyBundle()
      grepBundle <- ComponentRegistry.grepApiClient.getGrepBundle()
    } yield (taxonomyBundle, grepBundle)

    val start = System.currentTimeMillis()

    bundles match {
      case Failure(ex) => throw ex
      case Success((taxonomyBundle, grepBundle)) =>
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(SearchApiProperties.SearchIndexes.size))

        def reindexWithIndexService[C <: Content](indexService: ComponentRegistry.IndexService[C])(
            implicit mf: Manifest[C]): Future[Try[ReindexResult]] = {
          val reindexFuture = Future { indexService.indexDocuments(taxonomyBundle, grepBundle) }
          reindexFuture.onComplete {
            case Success(Success(reindexResult: ReindexResult)) =>
              logger.info(
                s"Completed indexing of ${reindexResult.totalIndexed} ${indexService.searchIndex} in ${reindexResult.millisUsed} ms.")
            case Success(Failure(ex)) => logger.warn(ex.getMessage, ex)
            case Failure(ex) =>
              logger.warn(s"Unable to create index '${indexService.searchIndex}': " + ex.getMessage, ex)
          }

          reindexFuture
        }

        val result = Await.result(
          Future.sequence(
            Seq(
              reindexWithIndexService(ComponentRegistry.learningPathIndexService),
              reindexWithIndexService(ComponentRegistry.articleIndexService),
              reindexWithIndexService(ComponentRegistry.draftIndexService)
            )),
          Duration.Inf
        )

        result.foreach(x =>
          if (x.isFailure) {
            logger.error("Indexing failed...")
            sys.exit(1)
        })

        logger.info(s"Reindexing all indexes took ${System.currentTimeMillis() - start} ms...")
        sys.exit(0)
    }
  }

}
