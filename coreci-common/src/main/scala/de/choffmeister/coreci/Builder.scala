package de.choffmeister.coreci

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import de.choffmeister.coreci.models._
import reactivemongo.bson._
import spray.json.JsString

import scala.concurrent._

class Builder(db: Database)
    (implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) {
  val config = Config.load()
  val docker = Docker.open(config.dockerWorkers)

  def run(pending: Build, dockerfile: Dockerfile): Future[Build] = {
    val startedAt = now
    val finished = for {
      running <- db.builds.update(pending.copy(status = Running(startedAt)))
      stream <- docker.build(dockerfile.asTar, running.id.stringify)
      finished <- withIndex(stream).mapAsync[Either[String, Output]] {
        case (i, StatusStream(content)) =>
          db.outputs.insert(Output(buildId = running.id, index = i, content = content)).map(Right.apply)
        case (i, OutputStream(content)) =>
          db.outputs.insert(Output(buildId = running.id, index = i, content = content)).map(Right.apply)
        case (i, ErrorStream(message)) =>
          Future(Left(message))
      }.runFold(running) {
        case (build@Build(_, _, _, Running(startedAt), _, _, _), Left(errorMessage)) =>
          build.copy(status = Failed(startedAt, now, errorMessage))
        case (build, _) =>
          build
      }.map {
        case build@Build(_, _, _, Running(startedAt), _, _, _) =>
          build.copy(status = Succeeded(startedAt, now))
        case build =>
          build
      }
      saved <- db.builds.update(finished)
    } yield saved

    finished.recoverWith {
      case err =>
        val message = Option(err.getMessage).getOrElse("Unknown error")
        db.builds.update(pending.copy(status = Failed(startedAt, now, message)))
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

  private def now = BSONDateTime(System.currentTimeMillis)
}
