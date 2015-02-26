package de.choffmeister.coreci.http

import akka.actor.ActorSystem
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.models._

import scala.concurrent.ExecutionContext

class BuildRoutes(val database: Database)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val routes =
    pathEnd {
      get {
        complete(database.builds.all)
      }
    }
}
