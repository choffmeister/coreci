package de.choffmeister.coreci.http

import akka.actor._
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.models._

import scala.concurrent.ExecutionContext

class ApiRoutes(val database: Database)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val authRoutes = new AuthRoutes(database)
  lazy val buildRoutes = new BuildRoutes(database)
  lazy val integrationRoutes = new IntegrationRoutes(database)

  lazy val routes =
    pathPrefix("auth")(authRoutes.routes) ~
    pathPrefix("builds")(buildRoutes.routes) ~
    pathPrefix("integrations")(integrationRoutes.routes)
}
