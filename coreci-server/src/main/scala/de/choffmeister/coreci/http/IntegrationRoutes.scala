package de.choffmeister.coreci.http

import akka.actor.ActorSystem
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.integrations.GitHubIntegration
import de.choffmeister.coreci.models.Database

import scala.concurrent.ExecutionContext

class IntegrationRoutes(val database: Database)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val integrations = List(new GitHubIntegration())

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
