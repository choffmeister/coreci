package de.choffmeister.coreci

import de.choffmeister.coreci.managers.UserManager
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
      jobs <- seq((1 to 3).map(i => db.jobs.insert(job(users.head, i))))
      builds <- seq((1 to 3).map(i => db.builds.insert(build(jobs.head, i))))
      outputs <- seq((1 to 3).map(i => db.outputs.insert(output(builds.head, i))))
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

  private def job(user: User, i: Int) = Job(
    userId = user.id,
    description = s"This is Job #$i")

  private def build(job: Job, i: Int) = Build(
    jobId = Some(job.id),
    status = Succeeded(now, now))

  private def output(build: Build, i: Int) = Output(
    buildId = build.id,
    index = i,
    content = s"Output line $i\n")

  private def now = BSONDateTime(System.currentTimeMillis)
  private def seq[T](fs: Seq[Future[T]])(implicit ec: ExecutionContext) = Future.sequence(fs)
}
