package de.choffmeister.coreci.integrations

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.marshallers.sprayjson.SprayJsonSupport
import akka.http.model.HttpMethods._
import akka.http.model.StatusCodes._
import akka.http.model._
import akka.http.server.Directives._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import de.choffmeister.coreci.Docker
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent._

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
          val fullName = payload.fields("repository").asJsObject.fields("full_name").toString()
          val archiveUrlTemplate = payload.fields("repository").asJsObject.fields("archive_url").toString()
          val after = payload.fields("after").toString()
          val docker = new Docker("localhost", 2375)
          val archive = archiveTarball(archiveUrlTemplate, after)
          val stream = archive.map(docker.build(_, fullName))

          complete(OK)
        }
      }
    }

  def archiveTarball(archiveUrlTemplate: String, ref: String): Future[Source[ByteString]] = {
    val archiveUrl = Uri(archiveUrlTemplate
      .replace("{archive_format}", "tarball")
      .replace("{/ref}", "/" + ref))
    log.debug("Loading archive tarball from {}", archiveUrl)

    val host = archiveUrl.authority.host.address()
    val port = archiveUrl.effectivePort
    val req = HttpRequest(GET, archiveUrl.toRelative)
    Source.single(req).via(Http().outgoingConnection(host, port).flow)
      .runWith(Sink.head)
      .map(_.entity.dataBytes)
  }
}
