package de.choffmeister.coreci

import java.io.File
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config => RawConfig, ConfigException, ConfigFactory}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

case class Config(
  httpInterface: String,
  httpPort: Int,
  mongoDbServers: List[(String, Int)],
  mongoDbDatabaseName: String,
  passwordHashAlgorithm: String,
  passwordHashAlgorithmConfig: List[String],
  webDir: Option[File])

object Config {
  def load(): Config = {
    val raw = ConfigFactory.load().getConfig("coreci")

    Config(
      httpInterface = raw.getString("http.interface"),
      httpPort = raw.getInt("http.port"),
      mongoDbServers = (raw.getString("mongodb.host"), raw.getInt("mongodb.port")) :: Nil,
      mongoDbDatabaseName = raw.getString("mongodb.database"),
      passwordHashAlgorithm = raw.getString("passwords.hash-algorithm").split(":", -1).toList.head,
      passwordHashAlgorithmConfig = raw.getString("passwords.hash-algorithm").split(":", -1).toList.tail,
      webDir = raw.getOptionalString("web-dir").map(new File(_))
    )
  }

  implicit class RichConfig(val underlying: RawConfig) extends AnyVal {
    def getOptionalString(path: String): Option[String] = try {
      Some(underlying.getString(path))
    } catch {
      case e: ConfigException.Missing => None
    }

    def getFiniteDuration(path: String): FiniteDuration = {
      val unit = TimeUnit.MICROSECONDS
      FiniteDuration(underlying.getDuration(path, unit), unit)
    }

    def getStringMap(path: String): Map[String, String] = {
      underlying.getConfig(path).root.asScala.toMap.map(e => e._1 -> e._2.unwrapped.asInstanceOf[String])
    }
  }
}
