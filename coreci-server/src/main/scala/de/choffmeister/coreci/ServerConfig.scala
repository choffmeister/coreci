package de.choffmeister.coreci

import java.io.File

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration

case class ServerConfig(
  httpInterface: String,
  httpPort: Int,
  webDir: Option[File])

object ServerConfig {
  import de.choffmeister.coreci.RichConfig._

  def load(): ServerConfig = {
    val raw = ConfigFactory.load().getConfig("coreci")

    ServerConfig(
      httpInterface = raw.getString("http.interface"),
      httpPort = raw.getInt("http.port"),
      webDir = raw.getOptionalString("web-dir").map(new File(_))
    )
  }
}
