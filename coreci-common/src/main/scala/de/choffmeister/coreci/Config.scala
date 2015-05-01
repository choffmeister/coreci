package de.choffmeister.coreci

import akka.http.scaladsl.model.Uri
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

object Config extends Logger {
  def load(): Config = {
    import de.choffmeister.coreci.RichConfig._
    val raw = TypesafeConfigFactory.load()
    val rawCoreci = raw.getConfig("coreci")

    Config(
      mongoDbServers = (rawCoreci.getString("mongodb.host") :: Nil).map(parseMongoDbHost),
      mongoDbDatabaseName = rawCoreci.getString("mongodb.database"),
      dockerWorkers = (rawCoreci.getString("docker.host") :: Nil).map(parseDockerHost),
      passwordHashAlgorithm = rawCoreci.getString("passwords.hash-algorithm").split(":", -1).toList.head,
      passwordHashAlgorithmConfig = rawCoreci.getString("passwords.hash-algorithm").split(":", -1).toList.tail,
      builderOutputGroupMaxCount = rawCoreci.getInt("builder.output-group.max-count"),
      builderOutputGroupMaxDuration = rawCoreci.getFiniteDuration("builder.output-group.max-count"),
      raw = raw
    )
  }

  private def parseMongoDbHost(str: String): (String, Int) = Uri(str) match {
    case Uri("mongodb", authority, Uri.Path.Empty | Uri.Path.SingleSlash, Uri.Query.Empty, None) =>
      (authority.host.address(), if (authority.port > 0) authority.port else 27017)
    case _ =>
      throw new Exception(s"Unsupported URI '$str' for MongoDB host")
  }

  private def parseDockerHost(str: String): (String, Int) = Uri(str) match {
    case Uri("tcp", authority, Uri.Path.Empty | Uri.Path.SingleSlash, Uri.Query.Empty, None) =>
      (authority.host.address(), if (authority.port > 0) authority.port else 2375)
    case _ =>
      throw new Exception(s"Unsupported URI '$str' for MongoDB host")
  }
}
