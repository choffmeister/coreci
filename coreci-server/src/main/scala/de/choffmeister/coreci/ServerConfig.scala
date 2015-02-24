package de.choffmeister.coreci

import java.io.File
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config => RawConfig, ConfigException, ConfigFactory}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

case class ServerConfig(
  httpInterface: String,
  httpPort: Int,
  webDir: Option[File])

object ServerConfig {
  def load(): ServerConfig = {
    val raw = ConfigFactory.load().getConfig("coreci")

    ServerConfig(
      httpInterface = raw.getString("http.interface"),
      httpPort = raw.getInt("http.port"),
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
