package de.choffmeister.coreci.http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._
import de.choffmeister.coreci.managers._
import de.choffmeister.coreci.models._
import reactivemongo.bson.BSONObjectID
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext

trait Routes extends JsonProtocol {
  implicit val system: ActorSystem
  implicit val executor: ExecutionContext
  implicit val materializer: FlowMaterializer
  val database: Database
  val routes: Route

  val config = Config.load()
  val serverConfig = ServerConfig.load()
  val userManager = new UserManager(config.passwordHashAlgorithm, config.passwordHashAlgorithmConfig, database)
  val authenticate = new Authenticator[User](
    serverConfig.authRealm,
    serverConfig.authBearerTokenSecret,
    id => database.users.find(BSONObjectID(id)),
    username => database.users.findByUserName(username),
    (user, password) => userManager.validateUserPassword(user, password))

  def viewable[T <: BaseModel](name: String, table: Table[T])(implicit jsonFormat: RootJsonFormat[T]): Route =
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

  def editable[T <: BaseModel](name: String, table: Table[T])(implicit jsonFormat: RootJsonFormat[T]): Route =
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

  def pageable: Directive1[(Option[Int], Option[Int])] = {
    parameters('skip.as[Int].?, 'limit.as[Int].?).tflatMap { res =>
      provide((res._1, res._2))
    }
  }
}
