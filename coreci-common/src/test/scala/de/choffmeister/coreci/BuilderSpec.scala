package de.choffmeister.coreci

import de.choffmeister.coreci.models._
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import reactivemongo.bson.BSONObjectID

class BuilderSpec extends Specification with NoTimeConversions {
  "Builder" should {
    "runs successful builds" in new TestActorSystem {
      TestDatabase { db =>
        val builder = new Builder(db)
        val project = await(db.projects.insert(Project(
          userId = BSONObjectID.generate,
          canonicalName = "p",
          title = "Project",
          description = "This is a project",
          dockerRepository = "busybox:latest",
          command = "uname" :: "-a" :: Nil)))
        val pending = await(db.builds.insert(Build(projectId = project.id, image = project.dockerRepository, command = project.command)))
        val finished = await(builder.run(pending))
        val outputs = await(db.outputs.all)

        finished.status must beAnInstanceOf[Succeeded]
        outputs.map(_.content).mkString must contain("GNU/Linux")
      }
    }

    "runs failing builds" in new TestActorSystem {
      TestDatabase { db =>
        val builder = new Builder(db)
        val project = await(db.projects.insert(Project(
          userId = BSONObjectID.generate,
          canonicalName = "p",
          title = "Project",
          description = "This is a project",
          dockerRepository = "busybox:latest",
          command = "false" :: Nil)))
        val pending = await(db.builds.insert(Build(projectId = project.id, image = project.dockerRepository, command = project.command)))
        val finished = await(builder.run(pending))

        finished.status must beAnInstanceOf[Failed]
        finished.status.asInstanceOf[Failed].errorMessage must contain("Exit code 1")
      }
    }

    "runs erroring builds" in new TestActorSystem {
      TestDatabase { db =>
        val builder = new Builder(db)
        val project = await(db.projects.insert(Project(
          userId = BSONObjectID.generate,
          canonicalName = "p",
          title = "Project",
          description = "This is a project",
          dockerRepository = "unknownimage",
          command = "uname" :: "-a" :: Nil)))
        val pending = await(db.builds.insert(Build(projectId = project.id, image = project.dockerRepository, command = project.command)))
        val finished = await(builder.run(pending))

        finished.status must beAnInstanceOf[Failed]
        finished.status.asInstanceOf[Failed].errorMessage must contain("No such image")
      }
    }
  }
}
