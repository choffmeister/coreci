package de.choffmeister.coreci

import akka.actor._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.models._

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

object WorkerHandlerProtocol {
  case object UpdateInfo
  case object UpdatePing
  case object DispatchBuild
}

case class Worker(info: Either[Int, DockerHostInfo], pings: Vector[(Long, Option[FiniteDuration])], state: WorkerState)

sealed trait WorkerState
case object IdleState extends WorkerState
case object DispatchingState extends WorkerState
case class RunningState(build: Build) extends WorkerState

class WorkerHandler(database: Database, workerUris: List[String])(implicit executor: ExecutionContext, materializer: FlowMaterializer) extends Actor with ActorLogging {
  import WorkerHandlerProtocol._
  implicit val system = context.system
  context.system.scheduler.schedule(0.second, 60.second, self, UpdateInfo)
  context.system.scheduler.schedule(0.second, 15.second, self, UpdatePing)
  context.system.scheduler.schedule(0.second, 5.second, self, DispatchBuild)

  val builder = new Builder(database)
  val workers = MutableMap(workerUris.map(uri => uri -> Worker(Left(0), Vector.empty, IdleState)): _*)

  def receive = {
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

    case UpdatePing =>
      workers.keys.foreach { uri =>
        val now = System.currentTimeMillis()
        Docker.open(uri).ping().map(Some.apply)
          .recover { case _ =>
            log.warning(s"Pinging worker $uri failed")
            None
          }
          .foreach { time =>
            if (time.isDefined) log.info(s"Pinging worker $uri took ${time.get.toMillis}ms")
            val old = workers(uri)
            workers.update(uri, old.copy(pings = capSize(old.pings :+ now -> time, 1000)))
          }
      }

    case DispatchBuild =>
      random(workers.filter(w => w._2.info.isRight && w._2.state == IdleState)).foreach { idleWorker =>
        workers.update(idleWorker._1, idleWorker._2.copy(state = DispatchingState))

        database.builds.getPending()
          .recover { case err =>
            log.warning(s"Failed to get pending build: $err")
            None
          }
          .foreach {
            case Some(pending) =>
              log.info(s"Dispatching build ${pending.projectCanonicalName}#${pending.number} (${pending.id.stringify}) to worker ${idleWorker._1}")
              workers.update(idleWorker._1, idleWorker._2.copy(state = RunningState(pending)))
              builder.run(pending).foreach { finished =>
                workers.update(idleWorker._1, idleWorker._2.copy(state = IdleState))
              }
            case None =>
              workers.update(idleWorker._1, idleWorker._2.copy(state = IdleState))
          }
      }
  }

  private def random[K, V](map: MutableMap[K, V]): Option[(K, V)] = map.size match {
    case 0 => None
    case l => Some(map.toSeq(Random.nextInt(l)))
  }

  private def capSize[T](list: Vector[T], max: Int): Vector[T] = list.size match {
    case l if l > max => list.drop(l - max)
    case _ => list
  }
}
