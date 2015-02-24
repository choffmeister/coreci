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
import de.choffmeister.coreci._
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent._

class GitHubIntegration(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer)
    extends Integration
    with DefaultJsonProtocol
    with SprayJsonSupport {
  lazy val log = LoggerFactory.getLogger(getClass)
  lazy val config = Config.load()

  val name = "github"
  val routes =
    path("hook") {
      post {
        entity(as[JsObject]) { payload =>
          val fullName = payload.fields("repository").asJsObject.fields("full_name").toString()
          val archiveUrlTemplate = payload.fields("repository").asJsObject.fields("archive_url").toString()
          val after = payload.fields("after").toString()
          val docker = Docker.open(config.dockerWorkers)
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

    get(archiveUrl, hopsRemaining = 1)
  }

  private def get(absoluteUri: Uri, headers: List[HttpHeader] = Nil, hopsRemaining: Int = 0): Future[Source[ByteString]] = {
    val host = absoluteUri.authority.host.address()
    val port = absoluteUri.effectivePort
    val relativeUri = absoluteUri.toRelative

    Source.single(HttpRequest(GET, relativeUri, headers))
      .via(Http().outgoingConnection(host, port).flow)
      .runWith(Sink.head)
      .flatMap { res =>
        res.status match {
          case OK =>
            Future(res.entity.dataBytes)
          case Found if hopsRemaining > 0 =>
            res.headers.find(_.is("location")).map(h => Uri(h.value())) match {
              case Some(location) =>
                get(location, headers, hopsRemaining - 1)
              case _ =>
                ???
            }
          case Found if hopsRemaining == 0 =>
            ???
          case _ =>
            ???
        }
      }
  }
}
