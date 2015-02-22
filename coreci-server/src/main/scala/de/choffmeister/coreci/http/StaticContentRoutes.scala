package de.choffmeister.coreci.http

import java.io.File

import akka.actor._
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._

import scala.concurrent.ExecutionContext

class StaticContentRoutes(webDir: Option[File])(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) extends JsonProtocol {
  lazy val routes =
    webDir.map(_.toString) match {
      case Some(webDir) =>
        val index = getFromFile(webDir + "/index.html")
        val cache = getFromFile(webDir + "/cache.manifest")
        val app = getFromDirectory(webDir)
        val routes =
          pathSingleSlash(index) ~
          path("index.html")(index) ~
          path("cache.manifest")(cache) ~
          pathPrefixTest("app" ~ Slash)(app) ~
          pathPrefixTest(!("app" ~ Slash))(index)
        routes
      case _ =>
        reject()
    }
}
