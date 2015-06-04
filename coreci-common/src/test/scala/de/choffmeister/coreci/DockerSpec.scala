package de.choffmeister.coreci

import akka.stream.scaladsl._
import akka.util.ByteString
import org.specs2.mutable._

import scala.concurrent.duration._

class DockerSpec extends Specification {
  val timeout = 60.seconds

  "Docker" should {
    "retrieve host version" in new TestActorSystem {
      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val future = docker.version()
        val result = await(future)

        result.version must startWith("1.")
      }
    }

    "retrieve host infos" in new TestActorSystem {
      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val future = docker.info()
        val result = await(future)

        result.cpus must beGreaterThanOrEqualTo(1)
      }
    }

    "ping host" in new TestActorSystem {
      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val future = docker.ping()
        val result = await(future)

        result.toMillis.toInt must beLessThan(5000)
      }
    }

    "build images" in new TestActorSystem {
      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val dockerfile = Dockerfile.from("busybox:latest")
          .run("uname -a")
          .run("echo hello world")

        val future = for {
          build <- docker.buildImage(Dockerfile.createTarBall(dockerfile), forceRemove = true, noCache = true)
          result <- build.stream.runFold(("", 0)) {
            case (acc, DockerBuildError(msg)) => (acc._1 + msg, acc._2 + 1)
            case (acc, o: DockerBuildOutput) => (acc._1 + o.message, acc._2)
          }.andThen { case _ => docker.deleteImage(build.imageName, force = true) }
        } yield result
        val result = await(future)

        result._1 must contain("GNU/Linux")
        result._1 must contain("hello world")
        result._2 === 0
      }

      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val dockerfile = Dockerfile.from("busybox:latest")
          .run("uname -a")
          .run("unknown command")

        val future = for {
          build <- docker.buildImage(Dockerfile.createTarBall(dockerfile), forceRemove = true, noCache = true)
          result <- build.stream.runFold(("", 0)) {
            case (acc, DockerBuildError(msg)) => (acc._1 + msg, acc._2 + 1)
            case (acc, o: DockerBuildOutput) => (acc._1 + o.message, acc._2)
          }.andThen { case _ => docker.deleteImage(build.imageName, force = true) }
        } yield result
        val result = await(future)

        result._1 must contain("GNU/Linux")
        result._2 === 1
      }
    }

    "build images with context" in new TestActorSystem {
      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val dockerfile = Dockerfile.from("busybox:latest")
          .add(".", "/context")
          .run("sh /context/test.sh")
        val context = Map("test.sh" -> ByteString("#!/bin/sh -e\n\necho hello\necho world\necho !!!"))

        val future = for {
          build <- docker.buildImage(Dockerfile.createTarBall(dockerfile, context), forceRemove = true, noCache = true)
          result <- build.stream.runFold(("", 0)) {
            case (acc, DockerBuildError(msg)) => (acc._1 + msg, acc._2 + 1)
            case (acc, o: DockerBuildOutput) => (acc._1 + o.message, acc._2)
          }.andThen { case _ => docker.deleteImage(build.imageName, force = true) }
        } yield result
        val result = await(future)

        result._1 must contain("hello")
        result._1 must contain("world")
        result._1 must contain("!!!")
        result._2 === 0
      }
    }

    "run images" in new TestActorSystem {
      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val command = "uname" :: "-a" :: Nil
        val future = for {
          run <- docker.runImage("busybox:latest", command, Map.empty)
          output <- run.stream.runFold("")(_ + _.utf8String).andThen { case _ => docker.deleteContainer(run.containerId, force = true) }
        } yield output
        val result = await(future)

        result must contain("GNU/Linux")
      }
    }

    "build, run and clean" in new TestActorSystem {
      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val tar = Dockerfile.createTarBall(Dockerfile.from("busybox:latest"), Map.empty)
        val future = docker.buildRunClean(tar, "uname" :: "-a" :: Nil, Map.empty).toMat(Sink.fold("")(_ + _))(_ zip _).run()
        val result = await(future)

        result._1._1 must beRight
        result._1._1.right.get.stateExitCode === 0
        result._1._2 must contain("GNU/Linux")
        result._1._2 === await(future)._2
      }

      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val tar = Dockerfile.createTarBall(Dockerfile.from("busybox:latest"), Map.empty)
        val future = docker.buildRunClean(tar, "sh" :: "unknowncmd" :: Nil, Map.empty).toMat(Sink.foreach(_ => ()))(Keep.left).run()
        val result = await(future)

        result._1 must beRight
        result._1.right.get.stateExitCode === 2
      }

      within(timeout) {
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val tar = Dockerfile.createTarBall(Dockerfile.from("unknownimage"), Map.empty)
        val future = docker.buildRunClean(tar, Nil, Map.empty).toMat(Sink.foreach(_ => ()))(Keep.left).run()
        val result = await(future)

        result._1 must beLeft
        result._2 must contain("unknownimage")
      }
    }
  }
}
