package de.choffmeister.coreci

import akka.stream.scaladsl._
import akka.util.ByteString
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

    "build images" in new TestActorSystem {
      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val dockerfile = Dockerfile.from("busybox:latest")
          .run("uname -a")
          .run("echo hello world")

        val future = for {
          build <- docker.buildImage(Dockerfile.toTarBall(dockerfile), forceRemove = true, noCache = true)
          result <- build.stream.runFold(("", 0)) {
            case (acc, DockerBuildStream(msg)) => (acc._1 + msg, acc._2)
            case (acc, DockerBuildStatus(msg)) => (acc._1 + msg, acc._2)
            case (acc, DockerBuildError(msg)) => (acc._1 + msg, acc._2 + 1)
          }.andThen { case _ => docker.deleteImage(build.imageName, force = true) }
        } yield result

        await(future)._1 must contain("GNU/Linux")
        await(future)._1 must contain("hello world")
        await(future)._2 === 0
      }

      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val dockerfile = Dockerfile.from("busybox:latest")
          .run("uname -a")
          .run("unknown command")

        val future = for {
          build <- docker.buildImage(Dockerfile.toTarBall(dockerfile), forceRemove = true, noCache = true).andThen { case x => println(x) }
          result <- build.stream.runFold(("", 0)) {
            case (acc, DockerBuildStream(msg)) => (acc._1 + msg, acc._2)
            case (acc, DockerBuildStatus(msg)) => (acc._1 + msg, acc._2)
            case (acc, DockerBuildError(msg)) => (acc._1 + msg, acc._2 + 1)
          }.andThen { case _ => docker.deleteImage(build.imageName, force = true) }
        } yield result

        await(future)._1 must contain("GNU/Linux")
        await(future)._2 === 1
      }
    }

    "build images with context" in new TestActorSystem {
      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val dockerfile = Dockerfile.from("busybox:latest")
          .add(".", "/context")
          .run("sh /context/test.sh")
        val context = Map("test.sh" -> ByteString("#!/bin/sh -e\n\necho hello\necho world\necho !!!"))

        val future = for {
          build <- docker.buildImage(Dockerfile.toTarBall(dockerfile, context), forceRemove = true, noCache = true)
          result <- build.stream.runFold(("", 0)) {
            case (acc, DockerBuildStream(msg)) => (acc._1 + msg, acc._2)
            case (acc, DockerBuildStatus(msg)) => (acc._1 + msg, acc._2)
            case (acc, DockerBuildError(msg)) => (acc._1 + msg, acc._2 + 1)
          }.andThen { case _ => docker.deleteImage(build.imageName, force = true) }
        } yield result

        await(future)._1 must contain("hello")
        await(future)._1 must contain("world")
        await(future)._1 must contain("!!!")
        await(future)._2 === 0
      }
    }

    "run images" in new TestActorSystem {
      within(5.seconds) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val command = "uname" :: "-a" :: Nil
        val future  = for {
          run <- docker.runImage("busybox:latest", command)
          output <- run.stream.runFold("")(_ + _.utf8String).andThen { case _ => docker.deleteContainer(run.containerId, force = true) }
        } yield output

        await(future) must contain("GNU/Linux")
      }
    }
  }
}
