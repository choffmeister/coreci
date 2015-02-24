package de.choffmeister.coreci.managers

import de.choffmeister.auth.common._
import de.choffmeister.coreci.models._
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, Future}

class UserManager(passwordHashAlgorithm: String, passwordHashAlgorithmConfig: List[String], db: Database)(implicit ec: ExecutionContext) {
  lazy val hasher = new PasswordHasher(passwordHashAlgorithm, passwordHashAlgorithmConfig, PBKDF2 :: Plain :: Nil)

  def createUser(user: User, password: String): Future[User] = {
    for {
      u <- db.users.insert(user)
      p <- changeUserPassword(u, password)
    } yield u
  }

  def changeUserPassword(user: User, newPassword: String): Future[UserPassword] = {
    db.userPasswords.insert(UserPassword(
      userId = user.id,
      createdAt = BSONDateTime(System.currentTimeMillis),
      password = hasher.hash(newPassword)))
  }

  def validateUserPassword(user: User, password: String): Future[Boolean] = {
    db.userPasswords.findCurrentPassword(user.id).map {
      case Some(up) => hasher.validate(up.password, password)
      case _ => false
    }
  }
}
