package de.choffmeister.coreci.http

import akka.actor._
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._
import de.choffmeister.coreci.integrations._
import de.choffmeister.coreci.models._

import scala.concurrent.ExecutionContext

class ApiRoutes(database: Database)(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) extends JsonProtocol {
  val github = new GitHubIntegration()
  val integrations = github :: Nil

  lazy val routes =
    pathPrefix("integrations") {
      pathPrefix(Segment) { name =>
        integrations.find(inte => inte.name == name) match {
          case Some(inte) =>
            path("info") {
              complete(inte.name)
            } ~
            inte.routes
          case None => reject
        }
      }
    }
}
