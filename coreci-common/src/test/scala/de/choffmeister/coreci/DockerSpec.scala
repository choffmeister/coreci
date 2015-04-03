package de.choffmeister.coreci

import akka.stream.scaladsl._
import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

class DockerSpec extends Specification with NoTimeConversions {
  "Docker" should {
    "create, start and attach to container" in new TestActorSystem {
      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers)
        val command = "uname" :: "-a" :: Nil
        val future = docker.runContainerWith("node:0.10", command, Sink.fold("")((acc, chunk) => acc + chunk._2.utf8String))

        await(future)._1 must contain("GNU/Linux")
      }
    }
  }
}
