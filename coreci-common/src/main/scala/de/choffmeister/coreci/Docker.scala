package de.choffmeister.coreci

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.actor._
import akka.stream.actor.ActorPublisherMessage._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import spray.json._

import scala.annotation.tailrec
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

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

sealed trait DockerBuildOutput {
  val message: String
}
case class DockerBuildStream(message: String) extends DockerBuildOutput
case class DockerBuildStatus(message: String) extends DockerBuildOutput
case class DockerBuildStatusProgress(message: String) extends DockerBuildOutput
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

  def buildRunClean(tar: Source[ByteString, Unit], command: List[String], environment: Map[String, String], maxOutputSize: Long = 1024 * 1024):
      Source[String, Future[(Either[Throwable, DockerContainerInspection], String)]] = {
    val buildOutputFlow = Flow[DockerBuildOutput]
      .map {
        case DockerBuildStream(msg) => ""
        case DockerBuildStatusProgress("Downloading") => ""
        case DockerBuildStatusProgress("Extracting") => ""
        case DockerBuildStatusProgress(msg) => msg + "\n"
        case DockerBuildStatus(msg) => msg + "\n"
        case DockerBuildError(msg) => msg + "\n"
      }
      .filter(_.nonEmpty)

    val runOutputFlow = Flow[ByteString]
      .map(_.utf8String)

    val promise = Promise[(Either[Throwable, DockerContainerInspection], String)]()
    Source.actorPublisher[String](Props(new ActorPublisher[String] {
      var buffer = Vector.empty[String]
      var bufferSize = 0L
      var bufferPosition = 0

      buildImage(tar, pull = true, forceRemove = true, noCache = true).flatMap { build =>
        build.stream.via(buildOutputFlow).runForeach(self ! _).flatMap { _ =>
          runImage(build.imageName, command, environment).flatMap { run =>
            run.stream.via(runOutputFlow).runForeach(self ! _).flatMap { _ =>
              inspectContainer(run.containerId)
            }.andThen { case _ => deleteContainer(run.containerId, force = true) }
          }
        }.andThen { case _ => deleteImage(build.imageName, force = true) }
      }.onComplete(self ! _)

      override def receive = {
        case Success(info: DockerContainerInspection) if bufferPosition == buffer.length =>
          onCompleteThenStop()
          promise.success((Right(info), buffer.fold("")(_ + _)))
        case Success(info) =>
          self ! Success(info)
        case Failure(err) =>
          onErrorThenStop(err)
          promise.trySuccess((Left(err), buffer.fold("")(_ + _)))
        case chunk: String =>
          if (bufferSize + chunk.length < maxOutputSize) {
            buffer :+= chunk
            bufferSize += chunk.length
            deliver()
          } else {
            val err = new Exception("Exceeded max buffer size")
            onErrorThenStop(err)
            promise.trySuccess((Left(err), buffer.fold("")(_ + _)))
          }
        case r @ Request(_) =>
          deliver()
        case c @ Cancel =>
          val err = new Exception("Cancelled subscription")
          context.stop(self)
          promise.trySuccess((Left(err), buffer.fold("")(_ + _)))
      }

      @tailrec
      final def deliver(): Unit = {
        if (bufferPosition < buffer.length && totalDemand > 0) {
          val next = buffer(bufferPosition)
          bufferPosition += 1
          onNext(next)
          deliver()
        }
      }
    })).mapMaterializedValue(_ => promise.future)
  }

  def buildImage(tar: Source[ByteString, Unit], name: Option[String] = None, pull: Boolean = false, forceRemove: Boolean = false, noCache: Boolean = false): Future[DockerBuild] = {
    log.info(s"Building image")
    val nameOrRandom = name.getOrElse(java.util.UUID.randomUUID().toString)
    val uri = Uri(s"/build?t=$nameOrRandom&pull=$pull&forcerm=$forceRemove&nocache=$noCache")
    val entity = HttpEntity.Chunked.fromData(ContentType(MediaTypes.`application/x-tar`), tar)

    val toBuildOutputStream = Flow[ByteString]
      .mapConcat { s =>
        try {
          val json = JsonParser(ParserInput(s.utf8String)).asJsObject
          if (json.fields.contains("stream")) {
            json.getFields("stream") match {
              case Seq(JsString(message)) => DockerBuildStream(message) :: Nil
              case _ => Nil
            }
          } else if (json.fields.contains("status") && json.fields.contains("progressDetail") && json.fields.contains("progress")) {
            json.getFields("status", "progressDetail", "progress") match {
              case Seq(JsString(message), JsObject(_), JsString(_)) => DockerBuildStatusProgress(message) :: Nil
              case _ => Nil
            }
          } else if (json.fields.contains("status")) {
            json.getFields("status") match {
              case Seq(JsString(message)) => DockerBuildStatus(message) :: Nil
              case _ => Nil
            }
          } else if (json.fields.contains("error")) {
            json.getFields("error") match {
              case Seq(JsString(message)) => DockerBuildError(message) :: Nil
              case _ => Nil
            }
          } else {
            log.warn(s"Unrecognized build output $s")
            Nil
          }
        } catch {
          case ex: JsonParser.ParsingException => DockerBuildError(s"Invalid output $s") :: Nil
        }
      }

    rawRequest(POST, uri, entity = entity).map { res =>
      val stream = res.entity.dataBytes.via(toBuildOutputStream)
      DockerBuild(nameOrRandom, stream)
    }
  }

  def deleteImage(name: String, force: Boolean = false, noPrune: Boolean = false): Future[Unit] = {
    log.info(s"Deleting image $name")
    jsonRequest(DELETE, Uri(s"/images/$name?force=$force&noprune=$noPrune")).map(_ => ())
  }

  def runImage[T](name: String, command: List[String], environment: Map[String, String]): Future[DockerRun] = {
    log.info(s"Running image $name")
    for {
      id <- createContainer(name, command, environment)
      s <- attachToContainer(id)
      _ <- startContainer(id)
    } yield DockerRun(id, s)
  }

  def createContainer(name: String, command: List[String], environment: Map[String, String]): Future[String] = {
    log.debug(s"Creating container from $name")
    val payload = JsObject(
      "Image" -> JsString(name),
      "Tty" -> JsBoolean(true),
      "Cmd" -> JsArray(command.map(JsString.apply).toVector),
      "Env" -> JsArray(environment.map(ev => ev._1 + "=" + ev._2).map(JsString.apply).toVector)
    )
    jsonRequest(POST, Uri("/containers/create"), Some(payload))
      .map(_.get.asJsObject)
      .map { json =>
        val id = json.fields("Id").asInstanceOf[JsString].value
        log.info(s"Created container $id from $name")
        id
      }
  }

  def deleteContainer(id: String, force: Boolean = false): Future[Unit] = {
    log.info(s"Deleting container $id")
    jsonRequest(DELETE, Uri(s"/containers/$id?force=$force")).map(_ => ())
  }

  def inspectContainer(id: String): Future[DockerContainerInspection] = {
    log.debug(s"Inspecting container $id")
    jsonRequest(GET, Uri(s"/containers/$id/json")).map(_.get.asJsObject).map(DockerJsonProtocol.readContainerInspection)
  }

  def startContainer(id: String): Future[Unit] = {
    log.info(s"Starting container $id")
    jsonRequest(POST, Uri(s"/containers/$id/start")).map(_ => ())
  }

  def attachToContainer(id: String): Future[Source[ByteString, Any]] = {
    log.debug(s"Attaching to container $id")
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
            case body if body.data.nonEmpty => Some(JsonParser(ParserInput(body.data.utf8String)))
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
