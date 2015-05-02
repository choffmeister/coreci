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
  version: Option[DockerVersion],
  hostInfo: Option[DockerHostInfo],
  pings: Vector[(Long, Option[FiniteDuration])],
  concurrency: Int,
  builds: List[Build],
  locked: Boolean)

sealed trait WorkerState
case object IdleState extends WorkerState
case object DispatchingState extends WorkerState
case class RunningState(build: Build) extends WorkerState

class WorkerHandler(database: Database, workerUris: List[String])
    (implicit executor: ExecutionContext, materializer: FlowMaterializer) extends Actor with ActorLogging {
  import WorkerHandlerProtocol._
  implicit val system = context.system

  context.system.scheduler.schedule(0.second, 60.second, self, UpdateVersionAndInfo)
  context.system.scheduler.schedule(0.second, 15.second, self, UpdatePing)
  context.system.scheduler.schedule(0.second, 5.second, self, DispatchBuild)

  val builder = new Builder(database)
  val workers = MutableMap(workerUris.map(uri => uri -> Worker(None, None, Vector.empty, 2, Nil, locked = false)): _*)

  def receive = {
    case UpdateVersionAndInfo =>
      workers.keys.foreach { uri =>
        log.debug(s"Fetching worker $uri info")
        val docker = Docker.open(uri)
        val f = for {
          version <- docker.version()
          info <- docker.info()
        } yield (version, info)

        f.onComplete {
          case Success((version, info)) =>
            log.debug(s"Updating worker $uri version and info")
            setVersion(uri, Some(version))
            setHostInfo(uri, Some(info))
          case Failure(err) =>
            log.error(s"Unable to fetch worker $uri version or info: $err")
            setVersion(uri, None)
            setHostInfo(uri, None)
        }
      }

    case UpdatePing =>
      workers.keys.foreach { uri =>
        Docker.open(uri).ping()
          .map(Some.apply)
          .recover { case _ => None }
          .map {
            case x @ Some(duration) => log.debug(s"Pinging worker $uri took ${duration.toMillis}ms"); x
            case x @ None => log.warning(s"Pinging worker $uri failed"); x
          }
          .foreach(duration => addPing(uri, duration))
      }

    case DispatchBuild =>
      val available = workers.filter(w => w._2.hostInfo.isDefined && w._2.builds.length < w._2.concurrency && !w._2.locked)

      random(available).foreach { case (uri, _) =>
        lock(uri)
        database.builds.getPending()
          .recover { case err =>
            log.warning(s"Failed to get pending build: $err")
            None
          }
          .foreach {
            case Some(build) =>
              log.debug(s"Dispatching build ${build.projectCanonicalName}#${build.number} (${build.id.stringify}) to worker $uri")
              addBuild(uri, build)
              builder.run(build).foreach { finished =>
                removeBuild(uri, build.id)
                self ! DispatchBuild
              }

              unlock(uri)
              self ! DispatchBuild
            case None =>
              unlock(uri)
          }
      }

    case QueryWorkers =>
      sender ! workers.toMap
  }

  private def addBuild(uri: String, build: Build) = {
    val w = workers(uri)
    workers.update(uri, w.copy(builds = w.builds :+ build))
  }

  private def removeBuild(uri: String, buildId: BSONObjectID) = {
    val w = workers(uri)
    workers.update(uri, w.copy(builds = w.builds.filter(_.id != buildId)))
  }

  private def lock(uri: String) = {
    workers.update(uri, workers(uri).copy(locked = true))
  }

  private def unlock(uri: String) = {
    workers.update(uri, workers(uri).copy(locked = false))
  }

  private def addPing(uri: String, duration: Option[FiniteDuration]) = {
    val w = workers(uri)
    val now = System.currentTimeMillis()
    workers.update(uri, w.copy(pings = capSize(w.pings :+ now -> duration, 1000)))
  }

  private def setVersion(uri: String, version: Option[DockerVersion]) = {
    val w = workers(uri)
    workers.update(uri, w.copy(version = version))
  }

  private def setHostInfo(uri: String, hostInfo: Option[DockerHostInfo]) = {
    val w = workers(uri)
    workers.update(uri, w.copy(hostInfo = hostInfo))
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
