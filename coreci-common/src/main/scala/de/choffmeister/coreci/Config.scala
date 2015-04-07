package de.choffmeister.coreci

import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory => TypesafeConfigFactory}

import scala.concurrent.duration.FiniteDuration

case class Config(
  mongoDbServers: List[(String, Int)],
  mongoDbDatabaseName: String,
  dockerWorkers: List[(String, Int)],
  passwordHashAlgorithm: String,
  passwordHashAlgorithmConfig: List[String],
  builderOutputGroupMaxCount: Int,
  builderOutputGroupMaxDuration: FiniteDuration,
  raw: TypesafeConfig)

object Config {
  def load(): Config = {
    import de.choffmeister.coreci.RichConfig._
    val raw = TypesafeConfigFactory.load()
    val rawCoreci = raw.getConfig("coreci")

    Config(
      mongoDbServers = (rawCoreci.getString("mongodb.host"), rawCoreci.getInt("mongodb.port")) :: Nil,
      mongoDbDatabaseName = rawCoreci.getString("mongodb.database"),
      dockerWorkers = (rawCoreci.getString("docker.host"), rawCoreci.getInt("docker.port")) :: Nil,
      passwordHashAlgorithm = rawCoreci.getString("passwords.hash-algorithm").split(":", -1).toList.head,
      passwordHashAlgorithmConfig = rawCoreci.getString("passwords.hash-algorithm").split(":", -1).toList.tail,
      builderOutputGroupMaxCount = rawCoreci.getInt("builder.output-group.max-count"),
      builderOutputGroupMaxDuration = rawCoreci.getFiniteDuration("builder.output-group.max-count"),
      raw = raw
    )
  }
}
