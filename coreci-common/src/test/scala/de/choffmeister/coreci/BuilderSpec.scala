package de.choffmeister.coreci

import de.choffmeister.coreci.models._
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import reactivemongo.bson.BSONObjectID

import scala.concurrent.duration._

class BuilderSpec extends Specification with NoTimeConversions {
  val timeout = 60.seconds

  "Builder" should {
    "runs successful builds" in new TestActorSystem {
      within(timeout)(TestDatabase(prefill = false) { db =>
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val builder = new Builder(db, docker)
        val project = await(db.projects.insert(Project(
          userId = BSONObjectID.generate,
          canonicalName = "p",
          title = "Project",
          description = "This is a project",
          image = "busybox:latest",
          script = "#!/bin/sh -e\n\nuname -a\n")))
        val pending = await(db.builds.insert(Build(projectId = project.id, image = project.image, script = project.script)))
        val finished = await(builder.run(pending))
        val outputs = await(db.outputs.all)

        finished.status must beAnInstanceOf[Succeeded]
        outputs.map(_.content).mkString must contain("GNU/Linux")
      })
    }

    "runs failing builds" in new TestActorSystem {
      within(timeout)(TestDatabase(prefill = false) { db =>
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val builder = new Builder(db, docker)
        val project = await(db.projects.insert(Project(
          userId = BSONObjectID.generate,
          canonicalName = "p",
          title = "Project",
          description = "This is a project",
          image = "busybox:latest",
          script = "#!/bin/sh -e\n\nexit 1")))
        val pending = await(db.builds.insert(Build(projectId = project.id, image = project.image, script = project.script)))
        val finished = await(builder.run(pending))

        finished.status must beAnInstanceOf[Failed]
        finished.status.asInstanceOf[Failed].errorMessage must contain("Exit code 1")
      })
    }

    "runs erroring builds" in new TestActorSystem {
      within(timeout)(TestDatabase(prefill = false) { db =>
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val builder = new Builder(db, docker)
        val project = await(db.projects.insert(Project(
          userId = BSONObjectID.generate,
          canonicalName = "p",
          title = "Project",
          description = "This is a project",
          image = "unknownimage",
          script = "#!/bin/sh -e\n\nuname -a\n")))
        val pending = await(db.builds.insert(Build(projectId = project.id, image = project.image, script = project.script)))
        val finished = await(builder.run(pending))

        finished.status must beAnInstanceOf[Failed]
        finished.status.asInstanceOf[Failed].errorMessage must contain("No such image")
      })
    }

    "inject environment into builds" in new TestActorSystem {
      within(timeout)(TestDatabase(prefill = false) { db =>
        val docker = Docker.open(Config.load().dockerWorkers.head._2)
        val builder = new Builder(db, docker)
        val project = await(db.projects.insert(Project(
          userId = BSONObjectID.generate,
          canonicalName = "p",
          title = "Project",
          description = "This is a project",
          image = "busybox:latest",
          environment =
            EnvironmentVariable("PUBLIC", "public", secret = false) ::
            EnvironmentVariable("WRAPPED", "\"wrapped\"", secret = false) ::
            EnvironmentVariable("SECRET", "secret", secret = true) :: Nil,
          script = "#!/bin/sh -e\n\necho \"print(PUBLIC)=$PUBLIC\"\necho \"print(WRAPPED)=$WRAPPED\"\n")))
        val pending = await(db.builds.insert(Build(projectId = project.id, image = project.image, script = project.script, environment = project.environment)))
        val finished = await(builder.run(pending))
        val outputs = await(db.outputs.all).map(_.content).mkString

        finished.status must beAnInstanceOf[Succeeded]
        outputs must contain("print(PUBLIC)=public")
        outputs must contain("print(WRAPPED)=\"wrapped\"")
        outputs must not contain("secret")
      })
    }
  }
}
