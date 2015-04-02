package de.choffmeister.coreci

import akka.actor._
import akka.http.Http
import akka.http.model.HttpEntity.Chunked
import akka.http.model.HttpMethods._
import akka.http.model._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import spray.json._

import scala.concurrent._

sealed trait DockerStream
case class StatusStream(message: String) extends DockerStream
case class OutputStream(content: String) extends DockerStream
case class ErrorStream(message: String) extends DockerStream

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
    (implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) extends Logger {
  def build(tar: Source[ByteString], repository: String, tag: Option[String] = None): Future[Source[DockerStream]] = {
    val fullName = repository + tag.map("%2F" + _).getOrElse("")
    val entity = Chunked.fromData(ContentType(MediaTypes.`application/x-tar`), tar)
    log.debug(s"Building $fullName from Dockerfile")

    val req = HttpRequest(POST, Uri("/build?t=" + fullName), entity = entity)
    Source.single(req).via(Http().outgoingConnection(host, port).flow)
      .runWith(Sink.head)
      .map { res =>
        res.entity.dataBytes.map(_.utf8String).map { s =>
          try {
            JsonParser(ParserInput(s)).asJsObject match {
              case s if s.fields.contains("status") =>
                StatusStream(s.fields("status").asInstanceOf[JsString].value)
              case s if s.fields.contains("stream") =>
                OutputStream(s.fields("stream").asInstanceOf[JsString].value)
              case s if s.fields.contains("error") =>
                ErrorStream(s.fields("error").asInstanceOf[JsString].value)
            }
          } catch {
            case ex: JsonParser.ParsingException => ErrorStream(s)
          }
        }
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
