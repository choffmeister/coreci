package de.choffmeister.coreci

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration

case class Config(
  mongoDbServers: List[(String, Int)],
  mongoDbDatabaseName: String,
  dockerWorkers: List[(String, Int)],
  passwordHashAlgorithm: String,
  passwordHashAlgorithmConfig: List[String],
  builderOutputGroupMaxCount: Int,
  builderOutputGroupMaxDuration: FiniteDuration)

object Config {
  def load(): Config = {
    import de.choffmeister.coreci.RichConfig._
    val raw = ConfigFactory.load().getConfig("coreci")

    Config(
      mongoDbServers = (raw.getString("mongodb.host"), raw.getInt("mongodb.port")) :: Nil,
      mongoDbDatabaseName = raw.getString("mongodb.database"),
      dockerWorkers = (raw.getString("docker.host"), raw.getInt("docker.port")) :: Nil,
      passwordHashAlgorithm = raw.getString("passwords.hash-algorithm").split(":", -1).toList.head,
      passwordHashAlgorithmConfig = raw.getString("passwords.hash-algorithm").split(":", -1).toList.tail,
      builderOutputGroupMaxCount = raw.getInt("builder.output-group.max-count"),
      builderOutputGroupMaxDuration = raw.getFiniteDuration("builder.output-group.max-count")
    )
  }
}
