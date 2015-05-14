package de.choffmeister.coreci.models

import de.choffmeister.coreci.TestDatabase
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import reactivemongo.bson._

import scala.concurrent._
import scala.concurrent.duration._

class DatabaseSpec extends Specification with NoTimeConversions{
  "Database" should {
    entity("users", _.users, noPreparation) {
      case Left((_, i)) => User(username = s"username$i", email = s"username$i@domain.com")
      case Right(u) => u.copy(username = u.username + "-modified")
    }

    entity("projects", _.projects, noPreparation) {
      case Left((_, i)) => Project(userId = BSONObjectID.generate, canonicalName = s"project$i", title = s"Project $i", description = s"This is projects $i", image = "", script = "")
      case Right(j) => j.copy(description = j.description + "-modified")
    }

    entity("builds", _.builds, withProject) {
      case Left((project, i)) => Build(projectId = project.id, status = Pending, image = "busybox:latest", script = "#!/bin/sh -e\n\nuname -a\n")
      case Right(b) => b.copy(status = Running(BSONDateTime(0)))
    }

    entity("outputs", _.outputs, noPreparation) {
      case Left((_, i)) => Output(buildId = BSONObjectID.generate, index = i, content = s"line$i")
      case Right(o) => o.copy(content = o.content + "-modified")
    }
  }

  private def noPreparation(db: Database): Unit = ()

  private def withProject(db: Database): Project =
    await(db.projects.insert(Project(userId = BSONObjectID.generate, canonicalName = "project", title = "Project", description = "This is a project", image = "", script = "")))

  private def await[T](f: => Future[T]): T =
    Await.result(f, 10.seconds)

  private def entity[T <: BaseModel, T2](name: String, table: Database => Table[T], prepare: Database => T2)(generator: Either[(T2, Int), T] => T) = {
    ("work with " + name) in TestDatabase(prefill = false) { db =>
      val tab = table(db)
      val prepared = prepare(db)

      val e1_in = generator(Left(prepared, 1))
      val e2_in = generator(Left(prepared, 2))

      await(tab.all) must beEmpty

      val e1_out = await(tab.insert(e1_in))
      e1_out.id !== e1_in.id
      await(tab.all) === List(e1_out)

      val e2_out = await(tab.insert(e2_in))
      e2_out.id !== e2_in.id
      await(tab.all) === List(e1_out, e2_out)

      val e2_mod_in = generator(Right(e2_out))
      e2_mod_in !== e2_out
      val e2_mod_out = await(tab.update(e2_mod_in))
      await(tab.all) === List(e1_out, e2_mod_out)

      await(tab.delete(e1_out))
      await(tab.all) === List(e2_mod_out)
    }
  }
}
