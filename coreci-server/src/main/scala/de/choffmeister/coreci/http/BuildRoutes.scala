package de.choffmeister.coreci.http

import akka.actor._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.choffmeister.coreci.WorkerHandlerProtocol
import de.choffmeister.coreci.models._

import scala.concurrent.ExecutionContext

class BuildRoutes(val database: Database, workerHandler: ActorRef)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: Materializer) extends Routes {
  lazy val routes =
    pathEnd {
      get {
        pageable { page =>
          complete(database.builds.list(page = page).map(_.map(_.defused)))
        }
      }
    } ~
    pathPrefix(BSONObjectIDSegment) { buildId =>
      onSuccess(database.builds.find(buildId)) {
        case Some(build) =>
          path("rerun") {
            post {
              authenticate() { user =>
                complete {
                  database.builds.insert(build.copy(status = Pending)).map { renewedBuild =>
                    workerHandler ! WorkerHandlerProtocol.DispatchBuild
                    renewedBuild.defused
                  }
                }
              }
            }
          }
        case None =>
          reject
      }
    }
}
