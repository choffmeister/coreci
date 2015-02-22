package de.choffmeister.coreci

import de.choffmeister.coreci.managers.UserManager
import de.choffmeister.coreci.models._

import scala.concurrent._

object TestDataGenerator {
  def generate(conf: Config, db: Database)(implicit executor: ExecutionContext): Future[Unit] = {
    val um = new UserManager(conf.passwordHashAlgorithm, conf.passwordHashAlgorithmConfig, db)

    for {
      user1 <- um.createUser(user(1), "pass1")
      user2 <- um.createUser(user(2), "pass2")
      user3 <- um.createUser(user(3), "pass3")
    } yield ()
  }

  private def user(i: Int) = User(
    username = s"user$i",
    email = s"user$i@domain.com")
}
