package de.choffmeister.coreci

import java.util.UUID

import de.choffmeister.coreci.models.Database
import org.specs2.execute._

import scala.concurrent.ExecutionContext.Implicits.global

object TestDatabase {
  lazy val config = Config.load()

  def apply[R: AsResult](a: Database ⇒ R) = {
    val prefix = UUID.randomUUID()
    val db = create(Some(prefix))
    try {
      AsResult.effectively(a(db))
    } finally {
      remove(prefix)
    }
  }

  def create(prefix: Option[UUID] = None): Database = {
    val prefixStr = prefix.getOrElse(UUID.randomUUID()).toString
    Database.open(config.mongoDbServers, config.mongoDbDatabaseName, prefixStr)
  }

  def remove(prefix: UUID): Unit = {
    // TODO implement
  }
}
