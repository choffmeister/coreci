package de.choffmeister.coreci

import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory => TypesafeConfigFactory}

import scala.concurrent.duration.FiniteDuration

case class Config(
  mongoDbServer: String,
  mongoDbDatabaseName: String,
  dockerWorkers: Map[String, (String, Boolean)],
  passwordHashAlgorithm: String,
  passwordHashAlgorithmConfig: List[String],
  builderOutputGroupMaxCount: Int,
  builderOutputGroupMaxDuration: FiniteDuration,
  raw: TypesafeConfig)

object Config extends Logger {
  def load(): Config = {
    import de.choffmeister.coreci.RichConfig._
    val raw = TypesafeConfigFactory.load()
    val rawCoreci = raw.getConfig("coreci")

    Config(
      mongoDbServer = rawCoreci.getString("mongodb.host"),
      mongoDbDatabaseName = rawCoreci.getString("mongodb.database"),
      dockerWorkers = Map("localhost" -> ("tcp://192.168.59.103:2376", true)),
      passwordHashAlgorithm = rawCoreci.getString("passwords.hash-algorithm").split(":", -1).toList.head,
      passwordHashAlgorithmConfig = rawCoreci.getString("passwords.hash-algorithm").split(":", -1).toList.tail,
      builderOutputGroupMaxCount = rawCoreci.getInt("builder.output-group.max-count"),
      builderOutputGroupMaxDuration = rawCoreci.getFiniteDuration("builder.output-group.max-count"),
      raw = raw
    )
  }
}
