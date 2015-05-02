package de.choffmeister.coreci

import akka.stream.scaladsl._
import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

class DockerSpec extends Specification with NoTimeConversions {
  "Docker" should {
    "retrieve host version" in new TestActorSystem {
      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val future = docker.version()

        await(future).version must startWith("1.")
      }
    }

    "retrieve host infos" in new TestActorSystem {
      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val future = docker.info()

        await(future)
        ok
      }
    }

    "ping host" in new TestActorSystem {
      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val future = docker.ping()

        await(future).toMillis.toInt must beLessThan(5000)
      }
    }

    "create, start and attach to container" in new TestActorSystem {
      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val command = "uname" :: "-a" :: Nil
        val future = docker.runContainerWith("busybox:latest", command, Sink.fold("")((acc, chunk) => acc + chunk._2.utf8String))

        await(future)._1 must contain("GNU/Linux")
      }
    }
  }
}
