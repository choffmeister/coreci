package de.choffmeister.coreci

import akka.actor._
import akka.http.Http
import akka.http.model.HttpEntity.Chunked
import akka.http.model.HttpMethods._
import akka.http.model._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import org.slf4j.LoggerFactory
import spray.json.{JsObject, JsonParser, ParserInput}

import scala.concurrent._

/**
 * Docker remote client
 * See https://docs.docker.com/reference/api/docker_remote_api_v1.17/
 *
 * @param host The host
 * @param port The port
 * @param system The actor system
 * @param executor The execution context
 * @param materializer The flow materializer
 */
class Docker(host: String, port: Int)
    (implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) {
  val log = LoggerFactory.getLogger(getClass)

  def build(tar: Source[ByteString], repository: String, tag: Option[String] = None): Future[Source[JsObject]] = {
    val fullName = repository + tag.map("%2F" + _).getOrElse("")
    val entity = Chunked.fromData(ContentType(MediaTypes.`application/x-tar`), tar)
    log.debug("Building {} from Dockerfile", fullName)

    val req = HttpRequest(POST, Uri("/build?t=" + fullName), entity = entity)
    Source.single(req).via(Http().outgoingConnection(host, port).flow)
      .runWith(Sink.head)
      .map { res =>
        res.entity.dataBytes.map(_.utf8String).map(s => JsonParser(ParserInput(s)).asJsObject)
      }
  }
}

object Docker {
  def open(workers: List[(String, Int)])
      (implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer): Docker = {
    // TODO implement some kind of load balacing
    val host = workers.head._1
    val port = workers.head._2
    new Docker(host, port)
  }
}
