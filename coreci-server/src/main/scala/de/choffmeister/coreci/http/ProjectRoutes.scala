package de.choffmeister.coreci.http

import akka.actor.ActorSystem
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._
import de.choffmeister.coreci.models._

import scala.concurrent.ExecutionContext

class ProjectRoutes(val database: Database)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val builder = new Builder(database)

  lazy val routes =
    pathEnd {
      get {
        complete(database.projects.all)
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
                  val dockerfile = Dockerfile.parse(project.dockerfile)
                  val pending = database.builds.insert(Build(projectId = project.id))
                  pending.flatMap(p => builder.run(p, dockerfile))
                  pending
                }
              }
            }
          } ~
          pathPrefix("builds") {
            pathEnd {
              get {
                complete(database.builds.listByProject(project.id))
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
                      complete(database.outputs.findByBuild(build.id).map(_.map(_.content).mkString))
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
