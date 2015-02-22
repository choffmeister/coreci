package de.choffmeister.coreci

import akka.actor._
import akka.http.Http
import akka.http.model.HttpEntity.Chunked
import akka.http.model._
import akka.stream.{FlattenStrategy, FlowMaterializer}
import akka.stream.scaladsl._
import akka.util.ByteString

import org.slf4j.LoggerFactory
import spray.json.{JsObject, ParserInput, JsonParser}

import scala.concurrent.ExecutionContext

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
class Docker(host: String, port: Int)(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) {
  val log = LoggerFactory.getLogger(getClass)
  val conn = Http().outgoingConnection(host, port)

  def build(tar: Source[ByteString], repository: String, tag: Option[String] = None): Source[JsObject] = {
    val fullname = repository + tag.map("%2F" + _).getOrElse("")
    val entity = Chunked.fromData(ContentType(MediaTypes.`application/x-tar`), tar)
    log.debug("Building {} from Dockerfile", fullname)

    Source(HttpRequest(HttpMethods.POST, Uri("/build?t=" + fullname), entity = entity) :: Nil).via(conn.flow)
      .map (_.entity.dataBytes.map(_.utf8String))
      .flatten(FlattenStrategy.concat)
      .map(s => JsonParser(ParserInput(s)).asJsObject)
  }
}
