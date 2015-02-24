package de.choffmeister.coreci.http

import akka.actor._
import akka.http.server.Directives._
import akka.http.server.Route
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._
import de.choffmeister.coreci.integrations._
import de.choffmeister.coreci.models._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext

class ApiRoutes(database: Database)(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) extends JsonProtocol {
  val github = new GitHubIntegration()
  val integrations = github :: Nil

  lazy val routes =
    viewable("users", database.users) ~
    viewable("jobs", database.jobs) ~
    viewable("builds", database.builds) ~
    viewable("outputs", database.outputs) ~
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

  private def viewable[T <: BaseModel](name: String, table: Table[T])(implicit jsonFormat: RootJsonFormat[T]): Route =
    pathPrefix(name) {
      pathEnd {
        get {
          complete(table.all)
        }
      } ~
      path(BSONObjectIDSegment) { id =>
        get {
          rejectEmptyResponse {
            complete(table.find(id))
          }
        }
      }
    }

  private def editable[T <: BaseModel](name: String, table: Table[T])(implicit jsonFormat: RootJsonFormat[T]): Route =
    viewable(name, table) ~
    pathPrefix(name) {
      pathEnd {
        post {
          entity(as[T]) { obj =>
            complete(table.insert(obj))
          }
        }
      } ~
      path(BSONObjectIDSegment) { id =>
        put {
          entity(as[T]) { obj =>
            complete(table.update(obj))
          }
        } ~
        delete {
          complete(table.delete(id).map(_ => id.stringify))
        }
      }
    }
}
