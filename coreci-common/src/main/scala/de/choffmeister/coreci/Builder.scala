package de.choffmeister.coreci

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import de.choffmeister.coreci.models._
import reactivemongo.bson._
import spray.json._

import scala.concurrent._
import scala.concurrent.duration._

class Builder(db: Database)
    (implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) extends Logger {
  val config = Config.load()
  val docker = Docker.open(config.dockerWorkers)

  def run(pending: Build, repository: String, command: List[String]): Future[Build] = {
    log.info(s"Running command $command on image $repository")

    val startedAt = now
    val outputSink = Flow[(Long, ByteString)]
      .groupedWithin(config.builderOutputGroupMaxCount, config.builderOutputGroupMaxDuration)
      .map { chunks =>
        (chunks.head._1, chunks.map(_._2).foldLeft(ByteString.empty)(_ ++ _).utf8String)
      }
      .mapAsync { case (index, content) =>
        db.outputs.insert(Output(buildId = pending.id, index = index, content = content))
      }
      .toMat(Sink.foreach(_ => ()))(Keep.right)

    val finished = for {
      started <- db.builds.update(pending.copy(status = Running(startedAt)))
      (res, info) <- docker.runContainerWith(repository, command, outputSink)
    } yield info.fields("State").asJsObject.fields("ExitCode").asInstanceOf[JsNumber].value.toInt match {
      case 0 => pending.copy(status = Succeeded(startedAt, now))
      case exitCode => pending.copy(status = Failed(startedAt, now, s"Exit code $exitCode"))
    }

    finished
      .recover { case err =>
        val message = Option(err.getMessage).getOrElse("Unknown error")
        pending.copy(status = Failed(startedAt, now, message))
      }
      .flatMap(db.builds.update)
  }

  private def now = BSONDateTime(System.currentTimeMillis)
}
