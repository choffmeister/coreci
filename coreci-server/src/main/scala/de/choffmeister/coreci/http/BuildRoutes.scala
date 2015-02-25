package de.choffmeister.coreci.http

import akka.actor.ActorSystem
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.models.{Build, Database}
import de.choffmeister.coreci.{Builder, Dockerfile}

import scala.concurrent.ExecutionContext

class BuildRoutes(val database: Database)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val builder = new Builder(database)

  lazy val routes =
    pathEnd {
      get {
        complete(database.builds.all)
      }
    } ~
    pathEnd {
      post {
        authenticate.bearerToken(acceptExpired = false) { user =>
          entity(as[String]) { raw =>
            complete {
              val dockerfile = Dockerfile.parse(raw)
              val pending = database.builds.insert(Build(jobId = None))
              pending.flatMap(p => builder.run(p, dockerfile))
              pending
            }
          }
        }
      }
    } ~
    path(BSONObjectIDSegment) { id =>
      get {
        rejectEmptyResponse {
          complete(database.builds.find(id))
        }
      }
    } ~
    path(BSONObjectIDSegment / "output") { buildId =>
      get {
        complete(database.outputs.findByBuild(buildId).map(_.map(_.content).mkString))
      }
    }
}
