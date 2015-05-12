package de.choffmeister.coreci

import java.util.concurrent.TimeUnit

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

case class DockerVersion(
  apiVersion: String,
  version: String,
  goVersion: String,
  gitCommit: String)

case class DockerHostInfo(
  id: String,
  name: String,
  cpus: Int,
  memory: Long,
  kernelVersion: String)

case class DockerContainerInspection(
  id: String,
  stateExitCode: Int)

sealed trait DockerBuildOutput
case class DockerBuildStream(message: String) extends DockerBuildOutput
case class DockerBuildStatus(message: String) extends DockerBuildOutput
case class DockerBuildError(message: String) extends DockerBuildOutput
case class DockerBuild(imageName: String, stream: Source[DockerBuildOutput, Any])

case class DockerRun(containerId: String, stream: Source[ByteString, Any])

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
  def version(): Future[DockerVersion] = {
    jsonRequest(GET, Uri("/version")).map(_.get.asJsObject).map(DockerJsonProtocol.readVersion)
  }

  def info(): Future[DockerHostInfo] = {
    jsonRequest(GET, Uri("/info")).map(_.get.asJsObject).map(DockerJsonProtocol.readHostInfo)
  }

  def ping(): Future[FiniteDuration] = {
    val start = System.nanoTime()
    jsonRequest(GET, Uri("/_ping")).map(_ => ()).map { _ =>
      val end = System.nanoTime()
      FiniteDuration(end - start, TimeUnit.NANOSECONDS)
    }
  }

  def buildImage[T](tar: Source[ByteString, Unit], name: Option[String] = None, forceRemove: Boolean = false, noCache: Boolean = false): Future[DockerBuild] = {
    val nameOrRandom = name.getOrElse(java.util.UUID.randomUUID().toString)
    val uri = Uri(s"/build?t=$nameOrRandom&forcerm=1&nocache=$noCache")
    val entity = HttpEntity.Chunked.fromData(ContentType(MediaTypes.`application/x-tar`), tar)

    rawRequest(POST, uri, entity = entity)
      .map { res =>
        val stream = res.entity.dataBytes.map(_.utf8String).map { s =>
          try {
            JsonParser(ParserInput(s)).asJsObject.fields.toList.sortBy(_._1) match {
              case ("status", JsString(message)) :: Nil => DockerBuildStatus(message)
              case ("stream", JsString(message)) :: Nil => DockerBuildStream(message)
              case ("error", JsString(message)) :: ("errorDetail", _) :: Nil => DockerBuildError(message)
              case _ => DockerBuildError(s"Invalid output $s")
            }
          } catch {
            case ex: JsonParser.ParsingException => DockerBuildError(s"Invalid output $s")
          }
        }
        DockerBuild(nameOrRandom, stream)
    }
  }

  def deleteImage(name: String, force: Boolean = false, noPrune: Boolean = false): Future[Unit] = {
    log.debug(s"Deleting image $name")
    jsonRequest(DELETE, Uri(s"/images/$name?force=$force&noprune=$noPrune")).map(_ => ())
  }

  def runImage[T](repository: String, command: List[String]): Future[DockerRun] = {
    for {
      id <- createContainer(repository, command)
      s <- attachToContainer(id)
      _ <- startContainer(id)
    } yield DockerRun(id, s)
  }

  def createContainer(repository: String, command: List[String]): Future[String] = {
    val payload = JsObject(
      "Image" -> JsString(repository),
      "Tty" -> JsBoolean(true),
      "Cmd" -> JsArray(command.map(JsString.apply).toVector)
    )

    log.debug(s"Creating container from $repository")
    jsonRequest(POST, Uri("/containers/create"), Some(payload))
      .map(_.get.asJsObject)
      .map { json =>
        val id = json.fields("Id").asInstanceOf[JsString].value
        log.info(s"Created container $id")
        id
      }
  }

  def deleteContainer(id: String, force: Boolean = false): Future[Unit] = {
    log.debug(s"Deleting container $id")
    jsonRequest(DELETE, Uri(s"/containers/$id?force=$force")).map(_ => ())
  }

  def inspectContainer(id: String): Future[DockerContainerInspection] = {
    jsonRequest(GET, Uri(s"/containers/$id/json")).map(_.get.asJsObject).map(DockerJsonProtocol.readContainerInspection)
  }

  def startContainer(id: String): Future[Unit] = {
    log.debug(s"Starting container $id")
    jsonRequest(POST, Uri(s"/containers/$id/start")).map(_ => ())
  }

  def attachToContainer(id: String): Future[Source[ByteString, Any]] = {
    rawRequest(POST, Uri(s"/containers/$id/attach?logs=true&stream=true&stdout=true&stderr=true")).map(_.entity.dataBytes)
  }

  private def rawRequest(
      method: HttpMethod,
      uri: Uri,
      entity: RequestEntity = HttpEntity.Empty): Future[HttpResponse] = {
    Source.single(HttpRequest(method, uri, entity = entity)).via(Http().outgoingConnection(host, port)).runWith(Sink.head)
  }

  private def jsonRequest(
      method: HttpMethod,
      uri: Uri,
      payload: Option[JsObject] = None,
      errorMap: PartialFunction[HttpResponse, Exception] = PartialFunction.empty): Future[Option[JsValue]] = {
    val entity = payload.map(p => HttpEntity(p.toString())).getOrElse(HttpEntity.Empty).withContentType(ContentTypes.`application/json`)

    Source.single(HttpRequest(method, uri, entity = entity)).via(Http().outgoingConnection(host, port))
      .runWith(Sink.head)
      .flatMap {
        case res if res.status.isSuccess() =>
          res.entity.toStrict(3.second).map {
            case body if body.data == ByteString("OK") => Some(JsString("OK"))
            case body if body.data.length > 0 => Some(JsonParser(ParserInput(body.data.utf8String)))
            case _ => None
          }
        case res =>
          res.toStrict(3.second).flatMap { res =>
            if (errorMap.isDefinedAt(res))
              Future.failed(errorMap(res))
            else
              res.entity.toStrict(3.second).flatMap(body => Future.failed(new Exception(body.data.utf8String)))
          }
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

object DockerJsonProtocol extends DefaultJsonProtocol {
  def readVersion(value: JsValue): DockerVersion =
    value.asJsObject.getFields("ApiVersion", "Version", "GitCommit", "GoVersion") match {
      case Seq(JsString(apiVersion), JsString(version), JsString(gitCommit), JsString(goVersion)) =>
        DockerVersion(
          apiVersion = apiVersion,
          version = version,
          gitCommit = gitCommit,
          goVersion = goVersion)
      case _ => jsonError("version")
    }

  def readHostInfo(value: JsValue): DockerHostInfo =
    value.asJsObject.getFields("ID", "Name", "NCPU", "MemTotal", "KernelVersion") match {
      case Seq(JsString(id), JsString(name), JsNumber(cpus), JsNumber(memory), JsString(kernelVersion)) =>
        DockerHostInfo(
          id = id,
          name = name,
          cpus = cpus.toInt,
          memory = memory.toLong,
          kernelVersion = kernelVersion)
      case _ => jsonError("host info")
    }

  def readContainerInspection(value: JsValue): DockerContainerInspection =
    value.asJsObject.getFields("Id", "State") match {
      case Seq(JsString(id), state: JsObject) =>
        state.getFields("ExitCode") match {
          case Seq(JsNumber(stateExitCode)) =>
            DockerContainerInspection(
              id = id,
              stateExitCode = stateExitCode.toInt)
          case _ => jsonError("container inspection")
        }
      case _ => jsonError("container inspection")
    }

  private def jsonError(expectedName: String) = throw new DeserializationException(s"Expected Docker $expectedName JSON object")
}
