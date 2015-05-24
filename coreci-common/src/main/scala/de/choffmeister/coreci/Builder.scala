package de.choffmeister.coreci

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.stream.stage.{SyncDirective, Context, PushPullStage}
import akka.util.ByteString
import de.choffmeister.coreci.models._
import reactivemongo.bson._

import scala.util._
import scala.concurrent._

class Builder(db: Database, docker: Docker)
    (implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) extends Logger {
  val config = Config.load()

  def run(pending: Build): Future[Build] = {
    log.info(s"Running build ${pending.id} on image ${pending.image}")
    val startedAt = now
    val dockerfile = Dockerfile.from(pending.image).add(".", "/coreci").run("chmod +x /coreci/build")
    val tar = Dockerfile.createTarBall(dockerfile, Map("build" -> ByteString(pending.script)))
    val environment = pending.environment.map(ev => ev.name -> ev.value).toMap

    for {
      started <- db.builds.update(pending.copy(status = Running(startedAt)))
      info <- docker.buildRunClean(tar, "/coreci/build" :: Nil, environment)
        .toMat(Sink.foreach(_ => ()))(Keep.left).run()
      finished <- Future(info)
        .flatMap {
          case (Right(i), o) if i.stateExitCode == 0 =>
            db.builds.update(pending.copy(status = Succeeded(startedAt, now), output = o))
          case (Right(i), o) =>
            db.builds.update(pending.copy(status = Failed(startedAt, now, s"Exit code ${i.stateExitCode}"), output = o))
          case (Left(err), o) =>
            db.builds.update(pending.copy(status = Failed(startedAt, now, s"Error ${err.getMessage}"), output = o))
      }
        .recoverWith {
          case e =>
            val message = Option(e.getMessage).getOrElse("Unknown error")
            db.builds.update(pending.copy(status = Failed(startedAt, now, message)))
        }
    } yield finished
  }

  private def now = BSONDateTime(System.currentTimeMillis)
}
