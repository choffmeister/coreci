package de.choffmeister.coreci

import java.util.UUID

import de.choffmeister.coreci.models.Database
import org.specs2.execute._

import scala.concurrent.ExecutionContext.Implicits.global

object TestDatabase {
  lazy val config = Config.load()

  def apply[R: AsResult](a: Database ⇒ R) = {
    val prefix = UUID.randomUUID().toString
    val db = Database.open(config.mongoDbServers, config.mongoDbDatabaseName, prefix)
    try {
      AsResult.effectively(a(db))
    } finally {
      // TODO remove collections
    }
  }
}
