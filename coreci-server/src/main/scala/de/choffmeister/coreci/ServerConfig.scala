package de.choffmeister.coreci

import java.io.File

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration

case class ServerConfig(
  httpInterface: String,
  httpPort: Int,
  authRealm: String,
  authBearerTokenSecret: Array[Byte],
  authBearerTokenLifetime: FiniteDuration,
  webDir: Option[File])

object ServerConfig {
  import de.choffmeister.coreci.RichConfig._

  def load(): ServerConfig = {
    val raw = ConfigFactory.load().getConfig("coreci")

    ServerConfig(
      httpInterface = raw.getString("http.interface"),
      httpPort = raw.getInt("http.port"),
      authRealm = raw.getString("auth.realm"),
      // TODO read and parse hexadecimal string
      authBearerTokenSecret = raw.getString("auth.bearer-token.secret").getBytes,
      authBearerTokenLifetime = raw.getFiniteDuration("auth.bearer-token.lifetime"),
      webDir = raw.getOptionalString("web-dir").map(new File(_))
    )
  }
}
