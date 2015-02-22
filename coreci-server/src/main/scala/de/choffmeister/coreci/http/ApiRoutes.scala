package de.choffmeister.coreci.http

import akka.actor._
import akka.http.model._
import akka.http.server.Route
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._
import de.choffmeister.coreci.models._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext

class ApiRoutes(database: Database)(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) extends JsonProtocol {
  lazy val routes =
    crud("users", database.users)

  private def crud[T <: BaseModel](name: String, table: Table[T])(implicit jsonFormat: RootJsonFormat[T]): Route =
    pathPrefix(name) {
      pathEnd {
        get {
          complete(table.all)
        } ~
        post {
          entity(as[T]) { obj =>
            complete(table.insert(obj))
          }
        }
      } ~
      path(BSONObjectIDSegment) { id =>
        get {
          rejectEmptyResponse {
            complete(table.find(id))
          }
        } ~
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
