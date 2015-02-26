package de.choffmeister.coreci.http

import akka.actor.ActorSystem
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.models._

import scala.concurrent.ExecutionContext

class JobRoutes(val database: Database)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val routes =
    pathEnd {
      get {
        complete(database.jobs.all)
      }
    } ~
    pathEnd {
      post {
        authenticate.bearerToken(acceptExpired = false) { user =>
          entity(as[Job]) { job =>
            complete(database.jobs.insert(job))
          }
        }
      }
    } ~
    pathPrefix(BSONObjectIDSegment) { jobId =>
      pathEnd {
        get {
          rejectEmptyResponse {
            complete(database.jobs.find(jobId))
          }
        }
      } ~
      pathPrefix("builds") {
        pathEnd {
          get {
            complete(database.builds.findByJob(jobId))
          }
        } ~
        pathPrefix(BSONObjectIDSegment) { buildId =>
          pathEnd {
            get {
              rejectEmptyResponse {
                complete(database.builds.find(buildId))
              }
            }
          } ~
          path("output") {
            get {
              complete(database.outputs.findByBuild(buildId).map(_.map(_.content).mkString))
            }
          }
        }
      }
    }
}
