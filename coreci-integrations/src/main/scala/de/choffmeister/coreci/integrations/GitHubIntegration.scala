package de.choffmeister.coreci.integrations

import akka.actor.ActorSystem
import akka.http.marshallers.sprayjson.SprayJsonSupport
import akka.http.model.StatusCodes
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.ExecutionContext

class GitHubIntegration(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer)
    extends Integration
    with DefaultJsonProtocol
    with SprayJsonSupport {
  val log = LoggerFactory.getLogger(getClass)
  val name = "github"
  val routes =
    path("hook") {
      post {
        entity(as[JsObject]) { payload =>
          log.debug(payload.prettyPrint)
          val ref = payload.fields("ref").toString()
          val sshUrl = payload.fields("repository").asJsObject.fields("ssh_url").toString()
          val before = payload.fields("before").toString()
          val after = payload.fields("after").toString()
          log.debug("Commit {}", after)

          complete(StatusCodes.OK)
        }
      }
    }
}
