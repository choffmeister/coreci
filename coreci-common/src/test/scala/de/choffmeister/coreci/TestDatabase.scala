package de.choffmeister.coreci

import java.util.UUID

import de.choffmeister.coreci.models.Database
import org.specs2.execute._

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object TestDatabase {
  lazy val config = Config.load()

  def apply[R: AsResult](a: Database ⇒ R) = {
    val suffix = UUID.randomUUID().toString
    val db = Database.open(config.mongoDbServers, config.mongoDbDatabaseName, suffix)

    try {
      await(db.configure())
      AsResult.effectively(a(db))
    } finally {
      await(db.clear())
    }
  }

  def await[T](f: Future[T]): T = Await.result(f, 10.seconds)
}
