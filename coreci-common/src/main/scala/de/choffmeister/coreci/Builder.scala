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
    val context = Dockerfile.createTarBall(dockerfile, Map("build" -> ByteString(pending.script)))

    def prepareOutputFlow(startIndex: Long) = Flow[DockerBuildOutput]
      .map {
        case DockerBuildStream(msg) => ""
        case DockerBuildStatusProgress("Downloading") => ""
        case DockerBuildStatusProgress("Extracting") => ""
        case DockerBuildStatusProgress(msg) => msg + "\n"
        case DockerBuildStatus(msg) => msg + "\n"
        case DockerBuildError(msg) => msg + "\n"
      }
      .groupedWithin(config.builderOutputGroupMaxCount, config.builderOutputGroupMaxDuration)
      .map(_.foldLeft("")(_ + _))
      .transform(() => new IndexStage(startIndex))
      .mapAsync(1) {
        case (cont, i) => db.outputs.insert(Output(buildId = pending.id, index = i, content = cont))
      }

    def runOutputFlow(startIndex: Long) = Flow[ByteString]
      .groupedWithin(config.builderOutputGroupMaxCount, config.builderOutputGroupMaxDuration)
      .map(_.foldLeft(ByteString.empty)(_ ++ _))
      .transform(() => new IndexStage(startIndex))
      .mapAsync(1) {
        case (cont, i) => db.outputs.insert(Output(buildId = pending.id, index = i, content = cont.utf8String))
      }

    val finished = for {
      started <- db.builds.update(pending.copy(status = Running(startedAt)))
      prepare <- docker.buildImage(context, pull = true, forceRemove = true, noCache = true)
      lastPrepareIndex <- prepare.stream.via(prepareOutputFlow(0L)).runFold(0L)((_, s) => s.index + 1)
      run <- docker.runImage(prepare.imageName, "/coreci/build" :: Nil, pending.environment.map(ev => ev.name -> ev.value).toMap)
      lastRunIndex <- run.stream.via(runOutputFlow(lastPrepareIndex)).runFold(0L)((_, s) => s.index)
      info <- docker.inspectContainer(run.containerId)
        .andThen { case _ => docker.deleteContainer(run.containerId, force = true) }
        .andThen { case _ => docker.deleteImage(prepare.imageName, force = true) }
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
