package de.choffmeister.coreci

import org.specs2.mutable._
import org.specs2.time.NoTimeConversions
import spray.json.JsObject

import scala.concurrent.duration._

class DockerSpec extends Specification with NoTimeConversions {
  "Docker" should {
    "builds image from dockerfile" in new TestActorSystem {
      within(60.seconds) {
        val docker = new Docker("localhost", 2375)
        val dockerfile = Dockerfile.from("ubuntu", Some("14.04"))
          .run("apt-get update")
          .run("apt-get install --yes nginx")
          .run("update-rc.d nginx disable")
          .run("echo 'daemon off;' >> /etc/nginx/nginx.conf")
          .cmd("/usr/sbin/nginx" :: Nil)
          .expose(80)
          .expose(443)
        val future = docker.build(dockerfile.asTar, "coreci/nginx").flatMap { messages =>
          messages.runFold(List.empty[JsObject])(_ :+ _)
        }

        await(future)
      }
    }
  }
}
