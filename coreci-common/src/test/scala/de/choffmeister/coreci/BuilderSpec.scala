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
        val builder = new Builder(db)
        val dockerfile = Dockerfile.from("ubuntu", Some("14.04"))
          .run("echo hello world")

        val pending = await(db.builds.insert(Build(jobId = None)))
        val finished = await(builder.run(pending, dockerfile))
        finished.status must beAnInstanceOf[Succeeded]
      }
    }

    "runs failing builds" in new TestActorSystem {
      TestDatabase { db =>
        val builder = new Builder(db)
        val dockerfile = Dockerfile.from("ubuntu", Some("14.04"))
          .run("unknowncommand")

        val pending = await(db.builds.insert(Build(jobId = None)))
        val finished = await(builder.run(pending, dockerfile))
        finished.status must beAnInstanceOf[Failed]
        finished.status.asInstanceOf[Failed].errorMessage must contain("unknowncommand")

        val outputs = await(db.outputs.findByBuild(finished.id))
        outputs(0).content === "Step 0 : FROM ubuntu:14.04\n"
        outputs(2).content === "Step 1 : RUN unknowncommand\n"
      }
    }

    "runs erroring builds" in new TestActorSystem {
      TestDatabase { db =>
        val builder = new Builder(db)
        val dockerfile = Dockerfile.parse("{}")

        val pending = await(db.builds.insert(Build(jobId = None)))
        val finished = await(builder.run(pending, dockerfile))
        finished.status must beAnInstanceOf[Failed]
        finished.status.asInstanceOf[Failed].errorMessage must contain("cannot continue")
      }
    }
  }
}
