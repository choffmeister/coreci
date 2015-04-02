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
      users <- seq((1 to 3).map(i => um.createUser(user(i), s"pass$i")))
      projects <- seq((1 to 3).map(i => db.projects.insert(project(users.head, i))))
      builds <- seq((1 to 3).map(i => db.builds.insert(build(projects.head, i))))
      outputs <- seq(builds.flatMap(b => (1 to 3).map(i => db.outputs.insert(output(b, i)))))
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
    dockerfile = i match {
      case 1 => Dockerfile
        .from("node", Some("0.10"))
        .workdir("/")
        .run("git clone https://github.com/choffmeister/gulp-reveal.git")
        .workdir("/gulp-reveal")
        .run("npm install -g gulp")
        .run("npm install")
        .run("gulp build")
        .asString
      case _ => Dockerfile
        .from("ubuntu", Some("14.04"))
        .run(s"echo this is project #$i")
        .asString
    })

  private def build(project: Project, i: Int) = Build(
    projectId = project.id,
    status = Succeeded(now, now))

  private def output(build: Build, i: Int) = Output(
    buildId = build.id,
    index = i,
    content = s"Output line $i of build ${build.number}\n")

  private def now = BSONDateTime(System.currentTimeMillis)
  private def seq[T](fs: Seq[Future[T]])(implicit ec: ExecutionContext) = Future.sequence(fs)
}
