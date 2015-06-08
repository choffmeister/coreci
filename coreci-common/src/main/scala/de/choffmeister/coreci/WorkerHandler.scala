package de.choffmeister.coreci

import akka.actor._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.models._
import reactivemongo.bson.BSONObjectID

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

object WorkerHandlerProtocol {
  case object UpdateVersionAndInfo
  case object UpdatePing
  case object DispatchBuild

  case object QueryWorkers
}

case class Worker(
  name: String,
  url: String,
  concurrency: Int,
  builds: List[Build],
  locked: Boolean,
  dockerVersion: Option[DockerVersion],
  dockerHostInfo: Option[DockerHostInfo],
  dockerPings: Vector[(Long, Option[FiniteDuration])])

sealed trait WorkerState
case object IdleState extends WorkerState
case object DispatchingState extends WorkerState
case class RunningState(build: Build) extends WorkerState

class WorkerHandler(database: Database, workerMap: Map[String, String])
    (implicit executor: ExecutionContext, materializer: FlowMaterializer) extends Actor with ActorLogging {
  import WorkerHandlerProtocol._
  implicit val system = context.system

  context.system.scheduler.schedule(0.second, 60.second, self, UpdateVersionAndInfo)
  context.system.scheduler.schedule(0.second, 15.second, self, UpdatePing)
  context.system.scheduler.schedule(0.second, 5.second, self, DispatchBuild)

  val workers = MutableMap(workerMap.map(w => w._1 -> Worker(w._1, w._2, 2, Nil, locked = false, None, None, Vector.empty)).toSeq: _*)

  def receive = {
    case UpdateVersionAndInfo =>
      workers.foreach { case (name, Worker(_, url, _, _, _, _, _, _)) =>
        log.debug(s"Fetching worker $name info")
        val docker = Docker.open(url)
        val f = for {
          version <- docker.version()
          info <- docker.info()
        } yield (version, info)

        f.onComplete {
          case Success((version, info)) =>
            log.debug(s"Updating worker $name version and info")
            setVersion(name, Some(version))
            setHostInfo(name, Some(info))
          case Failure(err) =>
            log.error(s"Unable to fetch worker $name version or info: $err")
            setVersion(name, None)
            setHostInfo(name, None)
        }
      }

    case UpdatePing =>
      workers.foreach { case (name, Worker(_, url, _, _, _, _, _, _)) =>
        val docker = Docker.open(url)
        docker.ping()
          .map(Some.apply)
          .recover { case _ => None }
          .map {
            case x @ Some(duration) => log.debug(s"Pinging worker $name took ${duration.toMillis}ms"); x
            case x @ None => log.warning(s"Pinging worker $name failed"); x
          }
          .foreach(duration => addPing(name, duration))
      }

    case DispatchBuild =>
      val available = workers.filter(w => w._2.dockerHostInfo.isDefined && w._2.builds.length < w._2.concurrency && !w._2.locked)

      random(available).foreach { case (name, Worker(_, url, _, _, _, _, _, _)) =>
        lock(name)
        database.builds.getPending()
          .recover { case err =>
            log.warning(s"Failed to get pending build: $err")
            None
          }
          .foreach {
            case Some(build) =>
              log.debug(s"Dispatching build ${build.projectCanonicalName}#${build.number} (${build.id.stringify}) to worker $name")
              addBuild(name, build)
              val docker = Docker.open(url)
              val builder = new Builder(database, docker)
              builder.runScript(build, build.script).foreach { finished =>
                removeBuild(name, build.id)
                self ! DispatchBuild
              }

              unlock(name)
              self ! DispatchBuild
            case None =>
              unlock(name)
          }
      }

    case QueryWorkers =>
      sender ! workers.values.toList
  }

  private def addBuild(name: String, build: Build) = {
    val w = workers(name)
    workers.update(name, w.copy(builds = w.builds :+ build))
  }

  private def removeBuild(name: String, buildId: BSONObjectID) = {
    val w = workers(name)
    workers.update(name, w.copy(builds = w.builds.filter(_.id != buildId)))
  }

  private def lock(name: String) = {
    workers.update(name, workers(name).copy(locked = true))
  }

  private def unlock(name: String) = {
    workers.update(name, workers(name).copy(locked = false))
  }

  private def addPing(name: String, duration: Option[FiniteDuration]) = {
    val w = workers(name)
    val now = System.currentTimeMillis()
    workers.update(name, w.copy(dockerPings = capSize(w.dockerPings :+ now -> duration, 1000)))
  }

  private def setVersion(name: String, version: Option[DockerVersion]) = {
    val w = workers(name)
    workers.update(name, w.copy(dockerVersion = version))
  }

  private def setHostInfo(name: String, hostInfo: Option[DockerHostInfo]) = {
    val w = workers(name)
    workers.update(name, w.copy(dockerHostInfo = hostInfo))
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
