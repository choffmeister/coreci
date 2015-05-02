package de.choffmeister.coreci.http

import akka.actor._
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._
import de.choffmeister.coreci.models._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class WorkerRoutes(val database: Database, workerHandler: ActorRef)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val routes =
    pathEnd {
      get {
        complete {
          val f = workerHandler.ask(WorkerHandlerProtocol.QueryWorkers)(1.seconds)
          f.mapTo[Map[String, Worker]]
        }
      }
    }
}
