package de.choffmeister.coreci

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import de.choffmeister.coreci.models._
import reactivemongo.bson._
import spray.json.JsString

import scala.concurrent._

class Builder(db: Database, dockerHost: String, dockerPort: Int)
    (implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) {
  val config = Config.load()
  val docker = Docker.open(config.dockerWorkers)

  def run(dockerfile: Dockerfile, job: Option[Job]): Future[Build] =
    for {
      pending <- db.builds.insert(Build(jobId = job.map(_.id)))
      running <- db.builds.update(pending.copy(status = Running(now)))
      stream <- docker.build(dockerfile.asTar, running.id.stringify)
      finished <- withIndex(stream).mapAsync[Either[String, Output]] {
        case (i, s) if s.fields.contains("stream") =>
          val content = s.fields("stream").asInstanceOf[JsString].value
          db.outputs.insert(Output(buildId = running.id, index = i, content = content)).map(Right.apply)
        case (i, s) if s.fields.contains("error") =>
          val error = s.fields("error").asInstanceOf[JsString].value
          Future(Left(error))
      }.runFold(running) {
        case (build@Build(_, _, Running(startedAt), _, _), Left(errorMessage)) =>
          build.copy(status = Failed(startedAt, now, errorMessage))
        case (build, _) =>
          build
      }.map {
        case build@Build(_, _, Running(startedAt), _, _) =>
          build.copy(status = Succeeded(startedAt, now))
        case build =>
          build
      }
      saved <- db.builds.update(finished)
    } yield saved

  private def withIndex[T](source: Source[T]): Source[(Long, T)] = {
    var index = 0L
    source.map { item =>
      val res = (index, item)
      index = index + 1
      res
    }
  }

  private def now = BSONDateTime(System.currentTimeMillis)
}
