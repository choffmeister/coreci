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
    log.info(s"Running command ${pending.command} on image ${pending.image}")
    val startedAt = now

    val outputFlow = Flow[ByteString]
      .groupedWithin(config.builderOutputGroupMaxCount, config.builderOutputGroupMaxDuration)
      .map(_.foldLeft(ByteString.empty)(_ ++ _))
      .transform(() => new IndexStage(0))
      .mapAsync(1) { case (content, index) =>
        db.outputs.insert(Output(buildId = pending.id, index = index, content = content.utf8String))
      }

    val finished = for {
      started <- db.builds.update(pending.copy(status = Running(startedAt)))
      run <- docker.runImage(pending.image, pending.command)
      done <- run.stream.via(outputFlow).runWith(Sink.fold(())((_, _) => ())).andThen { case _ => docker.deleteContainer(run.containerId) }
      info <- docker.inspectContainer(run.containerId)
      finished <- info.stateExitCode match {
        case 0 => db.builds.update(pending.copy(status = Succeeded(startedAt, now)))
        case exitCode => db.builds.update(pending.copy(status = Failed(startedAt, now, s"Exit code $exitCode")))
      }
    } yield finished

    finished
      .recoverWith { case err =>
        val message = Option(err.getMessage).getOrElse("Unknown error")
        db.builds.update(pending.copy(status = Failed(startedAt, now, message)))
      }
  }

  private def now = BSONDateTime(System.currentTimeMillis)
}

class IndexStage[T](startIndex: Long) extends PushPullStage[T, (T, Long)] {
  private var index = startIndex

  override def onPush(elem: T, ctx: Context[(T, Long)]): SyncDirective = {
    val current = index
    index = index + 1
    ctx.push((elem, current))
  }

  override def onPull(ctx: Context[(T, Long)]): SyncDirective = {
    ctx.pull()
  }
}
