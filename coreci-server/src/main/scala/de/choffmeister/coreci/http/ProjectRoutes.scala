package de.choffmeister.coreci.http

import akka.actor._
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._
import de.choffmeister.coreci.models._
import spray.json._

import scala.concurrent.ExecutionContext

class ProjectRoutes(val database: Database, workerHandler: ActorRef)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val routes =
    pathEnd {
      get {
        pageable { page =>
          complete(database.projects.list(page = page))
        }
      } ~
      post {
        authenticate.bearerToken(acceptExpired = false) { user =>
          entity(as[Project]) { project =>
            complete {
              database.projects.insert(project.copy(userId = user.id))
            }
          }
        }
      }
    } ~
    pathPrefix(Segment) { projectCanonicalName =>
      onSuccess(database.projects.findByCanonicalName(projectCanonicalName)) {
        case Some(project) =>
          pathEnd {
            get {
              complete(project)
            }
          } ~
          path("run") {
            post {
              authenticate.bearerToken(acceptExpired = false) { user =>
                complete {
                  database.builds.insert(Build(projectId = project.id, image = project.image, script = project.script)).map { build =>
                    workerHandler ! WorkerHandlerProtocol.DispatchBuild
                    build
                  }
                }
              }
            }
          } ~
          pathPrefix("builds") {
            pathEnd {
              get {
                pageable { page =>
                  complete(database.builds.listByProject(project.id, page))
                }
              }
            } ~
            pathPrefix(IntNumber) { buildNumber =>
              onSuccess(database.builds.findByNumber(project.id, buildNumber)) {
                case Some(build) =>
                  pathEnd {
                    get {
                      complete(build)
                    }
                  } ~
                  path("output") {
                    get {
                      pageable { page =>
                        complete {
                          database.outputs.findByBuild(build.id)
                            .map(_.foldLeft("")(_ + _.content))
                            .map(_.drop(page._1.getOrElse(0)).take(page._2.getOrElse(Int.MaxValue)))
                        }
                      }
                    }
                  }
                case None =>
                  reject
              }
            }
          }
        case None =>
          reject
      }
    }
}
