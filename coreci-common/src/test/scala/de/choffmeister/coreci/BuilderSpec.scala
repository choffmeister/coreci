package de.choffmeister.coreci

import de.choffmeister.coreci.models._
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._

class BuilderSpec extends Specification with NoTimeConversions {
  "Builder" should {
    "runs successful builds" in new TestActorSystem {
      TestDatabase { db =>
        val builder = new Builder(db, "localhost", 2375)
        val dockerfile = Dockerfile.from("ubuntu", Some("14.04"))
          .run("echo hello world")

        val build = Await.result(builder.run(dockerfile, None), Duration.Inf)
        build.status must beAnInstanceOf[Succeeded]
      }
    }

    "runs failing builds" in new TestActorSystem {
      TestDatabase { db =>
        val builder = new Builder(db, "localhost", 2375)
        val dockerfile = Dockerfile.from("ubuntu", Some("14.04"))
          .run("unknowncommand")

        val build = Await.result(builder.run(dockerfile, None), Duration.Inf)
        build.status must beAnInstanceOf[Failed]
        build.status.asInstanceOf[Failed].errorMessage must contain("unknowncommand")

        val outputs = await(db.outputs.findByBuild(build.id))
        outputs(0).content === "Step 0 : FROM ubuntu:14.04\n"
        outputs(2).content === "Step 1 : RUN unknowncommand\n"
      }
    }
  }
}
