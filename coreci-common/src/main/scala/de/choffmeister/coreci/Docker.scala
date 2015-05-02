package de.choffmeister.coreci

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import spray.json._

import scala.concurrent._
import scala.concurrent.duration._

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
  def runContainerWith[T](
      repository: String,
      command: List[String],
      sink: Sink[(Long, ByteString), Future[T]]): Future[(T, JsObject)] = {
    createContainer(repository, command).flatMap { id =>
      val future = for {
        s <- attachToContainer(id)
        _ <- startContainer(id)
        m <- withIndex(s).runWith(sink)
        i <- inspectContainer(id)
      } yield (m, i)

      future.onComplete { case _ => deleteContainer(id) }
      future
    }
  }

  def createContainer(repository: String, command: List[String]): Future[String] = {
    val payload = JsObject(
      "Image" -> JsString(repository),
      "Tty" -> JsBoolean(true),
      "Cmd" -> JsArray(command.map(JsString.apply).toVector)
    )

    log.debug(s"Creating container from $repository")
    request(POST, Uri("/containers/create"), Some(payload))
      .flatMap(parseResponseBodyAsJson)
      .map { json =>
        val id = json.fields("Id").asInstanceOf[JsString].value
        log.info(s"Created container $id")
        id
      }
  }

  def deleteContainer(id: String): Future[Unit] = {
    log.debug(s"Deleting container $id")
    request(DELETE, Uri(s"/containers/$id")).flatMap(drainResponseBody)
  }

  def inspectContainer(id: String): Future[JsObject] = {
    request(GET, Uri(s"/containers/$id/json")).flatMap(parseResponseBodyAsJson)
  }

  def startContainer(id: String): Future[Unit] = {
    log.debug(s"Starting container $id")
    request(POST, Uri(s"/containers/$id/start")).flatMap(drainResponseBody)
  }

  def attachToContainer(id: String): Future[Source[ByteString, Any]] = {
    request(POST, Uri(s"/containers/$id/attach?logs=true&stream=true&stdout=true&stderr=true")).map(_.entity.dataBytes)
  }

  private def parseResponseBodyAsJson(response: HttpResponse): Future[JsObject] = {
    response.entity.toStrict(10.seconds).map(_.data.utf8String).map { s =>
      JsonParser(ParserInput(s)).asJsObject
    }
  }

  private def drainResponseBody(response: HttpResponse): Future[Unit] = {
    response.entity.dataBytes.runFold(())((_, _) => ())
  }

  private def request(
      method: HttpMethod,
      uri: Uri,
      payload: Option[JsObject] = None,
      errorMap: PartialFunction[HttpResponse, Exception] = PartialFunction.empty): Future[HttpResponse] = {
    val ent = payload.map(p => HttpEntity(p.toString())).getOrElse(HttpEntity.Empty).withContentType(ContentTypes.`application/json`)
    val req = HttpRequest(method, uri, entity = ent)
    Source.single(req).via(Http().outgoingConnection(host, port))
      .runWith(Sink.head)
      .flatMap {
        case res if res.status.isSuccess() => Future(res)
        case res => res.toStrict(3.second).flatMap { res =>
          if (errorMap.isDefinedAt(res))
            Future.failed(errorMap(res))
          else
            res.entity.toStrict(3.second).flatMap(body => Future.failed(new Exception(body.data.utf8String)))
        }
      }
  }

  private def withIndex[T, Mat](source: Source[T, Mat]): Source[(Long, T), Mat] = {
    var index = 0L
    source.map { item =>
      val res = (index, item)
      index = index + 1
      res
    }
  }
}

object Docker {
  def open(uri: String)
      (implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer): Docker = {
    val (host, port) = parseUri(uri)
    new Docker(host, port)
  }

  private def parseUri(uri: String): (String, Int) = Uri(uri) match {
    case Uri("tcp", authority, Uri.Path.Empty | Uri.Path.SingleSlash, Uri.Query.Empty, None) =>
      (authority.host.address(), if (authority.port > 0) authority.port else 2375)
    case _ =>
      throw new Exception(s"Unsupported URI '$uri' for MongoDB host")
  }
}
