package de.choffmeister.coreci.http

import java.io.File

import akka.actor._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.choffmeister.coreci._

import scala.concurrent.ExecutionContext

class StaticContentRoutes(webDir: Option[File])(implicit system: ActorSystem, executor: ExecutionContext, materializer: Materializer) extends JsonProtocol {
  lazy val routes =
    webDir.map(_.toString) match {
      case Some(webDir) =>
        val index = getFromFile(webDir + "/index.html")
        val favicon = getFromFile(webDir + "/favicon.ico")
        val app = getFromDirectory(webDir)
        val routes =
          pathSingleSlash(index) ~
          path("index.html")(index) ~
          path("favicon.ico")(favicon) ~
          pathPrefixTest("app" ~ Slash)(app) ~
          pathPrefixTest(!("app" ~ Slash))(index)
        routes
      case _ =>
        reject()
    }
}
