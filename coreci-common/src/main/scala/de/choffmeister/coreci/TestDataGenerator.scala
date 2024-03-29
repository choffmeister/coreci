package de.choffmeister.coreci

import de.choffmeister.coreci.managers._
import de.choffmeister.coreci.models._
import reactivemongo.bson._

import scala.concurrent._

class TestDataGenerator(conf: Config, db: Database) extends Logger {
  def run()(implicit executor: ExecutionContext): Future[Unit] = {
    log.info("Creating test data")

    val um = new UserManager(conf.passwordHashAlgorithm, conf.passwordHashAlgorithmConfig, db)

    for {
      users <- seq((1 to 2).map(i => um.createUser(user(i), s"pass$i")))
      projects <- seq((1 to 2).map(i => db.projects.insert(project(users.head, i))))
      builds <- seq((1 to 2).map(i => db.builds.insert(build(projects.head, i))))
      outputs <- seq(builds.flatMap(b => (1 to 2).map(i => db.outputs.insert(output(b, i)))))
    } yield ()
  }

  private def user(i: Int) = User(
    username = s"user$i",
    email = s"user$i@domain.com")

  private def project(user: User, i: Int) = Project(
    userId = user.id,
    canonicalName = s"project$i",
    title = s"Project $i",
    description = s"This is Project #$i",
    image = "node:0.10",
    script = i match {
      case 1 => "#!/bin/bash\n\necho $PUBLIC\necho $PRIVATE\n\nnpm install -g gulp --no-spin\n"
      case _ => "#!/bin/bash\n\nuname -a\n"
    },
    environment = EnvironmentVariable("PUBLIC", "public", secret = false) :: EnvironmentVariable("PRIVATE", "private", secret = true) :: Nil)

  private def build(project: Project, i: Int) = Build(
    projectId = project.id,
    status = Succeeded(now, now),
    image = project.image,
    script = project.script,
    environment = project.environment)

  private def output(build: Build, i: Int) = Output(
    buildId = build.id,
    index = i,
    content = s"Output line $i of build ${build.number}\n")

  private def now = BSONDateTime(System.currentTimeMillis)
  private def seq[T](fs: Seq[Future[T]])(implicit ec: ExecutionContext) = Future.sequence(fs)
}
