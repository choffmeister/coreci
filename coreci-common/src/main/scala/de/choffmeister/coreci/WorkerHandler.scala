package de.choffmeister.coreci

import akka.actor._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.models._
import spray.json.JsObject

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

object WorkerHandlerProtocol {
  case object UpdateInfo
  case object DispatchBuild
}

case class Worker(info: Either[Int, JsObject], state: WorkerState)

sealed trait WorkerState
case object IdleState extends WorkerState
case object DispatchingState extends WorkerState
case class RunningState(build: Build) extends WorkerState

class WorkerHandler(database: Database, workerUris: List[String])(implicit executor: ExecutionContext, materializer: FlowMaterializer) extends Actor with ActorLogging {
  import WorkerHandlerProtocol._
  implicit val system = context.system
  context.system.scheduler.schedule(0.second, 60.second, self, UpdateInfo)
  context.system.scheduler.schedule(0.second, 5.second, self, DispatchBuild)

  val builder = new Builder(database)
  val workers = MutableMap(workerUris.map(uri => uri -> Worker(Left(0), IdleState)): _*)

  def receive = {
    case DispatchBuild =>
      random(workers.filter(w => w._2.info.isRight && w._2.state == IdleState)).foreach { idleWorker =>
        workers.update(idleWorker._1, idleWorker._2.copy(state = DispatchingState))

        database.builds.getPending()
          .recover { case err =>
            log.error(s"Failed to get pending build: $err")
            None
          }
          .foreach {
            case Some(pending) =>
              log.info(s"Dispatch build ${pending.projectCanonicalName}#${pending.number} (${pending.id.stringify}) to worker ${idleWorker._1}")
              workers.update(idleWorker._1, idleWorker._2.copy(state = RunningState(pending)))
              builder.run(pending).foreach { finished =>
                workers.update(idleWorker._1, idleWorker._2.copy(state = IdleState))
              }
            case None =>
              workers.update(idleWorker._1, idleWorker._2.copy(state = IdleState))
          }
      }

    case UpdateInfo =>
      workers.keys.foreach { uri =>
        log.debug(s"Fetching worker $uri info")
        Docker.open(uri).info().onComplete {
          case Success(info) =>
            log.debug(s"Updating worker $uri state")
            workers.update(uri, workers(uri).copy(info = Right(info)))
          case Failure(err) =>
            log.error(s"Unable to fetch worker $uri state: $err")
            workers(uri).info match {
              case Right(_) => workers.update(uri, workers(uri).copy(info = Left(1)))
              case Left(i) => workers.update(uri, workers(uri).copy(info = Left(i + 1)))
            }
        }
      }
  }

  private def random[K, V](map: MutableMap[K, V]): Option[(K, V)] = map.size match {
    case 0 => None
    case l => Some(map.toSeq(Random.nextInt(l)))
  }
}
