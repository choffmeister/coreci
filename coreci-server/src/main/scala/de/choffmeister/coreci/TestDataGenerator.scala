package de.choffmeister.coreci

import de.choffmeister.coreci.managers._
import de.choffmeister.coreci.models._
import org.slf4j.LoggerFactory
import reactivemongo.bson._

import scala.concurrent._
import scala.util.{Failure, Success}

object TestDataGenerator {
  val log = LoggerFactory.getLogger(getClass)

  def generate(conf: Config, db: Database)(implicit executor: ExecutionContext): Future[Unit] = {
    log.debug("Creating test data")

    val um = new UserManager(conf.passwordHashAlgorithm, conf.passwordHashAlgorithmConfig, db)

    val future: Future[Unit] = for {
      users <- seq((1 to 3).map(i => um.createUser(user(i), s"pass$i")))
      projects <- seq((1 to 3).map(i => db.projects.insert(project(users.head, i))))
      builds <- seq((1 to 3).map(i => db.builds.insert(build(projects.head, i))))
      outputs <- seq(builds.flatMap(b => (1 to 3).map(i => db.outputs.insert(output(b, i)))))
    } yield ()

    future.onComplete {
      case Success(_) => log.debug("Created test data")
      case Failure(err) => log.error("Failed at creating test data", err)
    }
    future
  }

  private def user(i: Int) = User(
    username = s"user$i",
    email = s"user$i@domain.com")

  private def project(user: User, i: Int) = Project(
    userId = user.id,
    canonicalName = s"project$i",
    title = s"Project $i",
    description = s"This is Project #$i",
    dockerfile = Dockerfile.from("ubuntu", Some("14.04")).run(s"echo this is project #$i").asString)

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
