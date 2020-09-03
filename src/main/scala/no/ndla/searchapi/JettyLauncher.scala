/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import java.util
import java.util.concurrent.Executors

import com.typesafe.scalalogging.LazyLogging
import javax.servlet.DispatcherType
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.io.Source

object JettyLauncher extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

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

    // Trigger reindexing if applicable
    implicit val indexThreadPool: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))
    Future {
      ComponentRegistry.draftIndexService.buildInitialIndex()
      ComponentRegistry.learningPathIndexService.buildInitialIndex()
      ComponentRegistry.articleIndexService.buildInitialIndex()
    }

    val server = new Server(SearchApiProperties.ApplicationPort)
    server.setHandler(context)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port ${SearchApiProperties.ApplicationPort} in $startTime ms.")

    server.join()
  }
}
