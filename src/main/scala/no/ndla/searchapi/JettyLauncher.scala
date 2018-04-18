/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source

object JettyLauncher extends LazyLogging {

  def main(args: Array[String]) {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    val startMillis = System.currentTimeMillis()

    val context = new ServletContextHandler()
    context setContextPath "/"
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    val server = new Server(SearchApiProperties.ApplicationPort)
    server.setHandler(context)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port ${SearchApiProperties.ApplicationPort} in $startTime ms.")

    server.join()
  }
}
