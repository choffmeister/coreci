package de.choffmeister.coreci.http

import java.util.Date

import akka.actor._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.choffmeister.auth.common._
import de.choffmeister.coreci.models._
import spray.json.JsString

import scala.concurrent.ExecutionContext

class AuthRoutes(val database: Database)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: Materializer) extends Routes {
  lazy val routes =
    pathPrefix("token") {
      path("create") {
        get {
          authenticate.basic() { user =>
            completeWithToken(user)
          }
        }
      } ~
      path("renew") {
        get {
          authenticate.bearerToken(acceptExpired = true) { user =>
            completeWithToken(user)
          }
        }
      }
    } ~
    path("state") {
      get {
        authenticate() { user =>
          complete(user)
        }
      }
    }

  private def completeWithToken(token: JsonWebToken): Route = {
    val secret = serverConfig.authBearerTokenSecret
    val lifetime = serverConfig.authBearerTokenLifetime.toSeconds
    val token2 = token.copy(expiresAt = new Date(System.currentTimeMillis / 1000L * 1000L + lifetime * 1000L))
    val response = OAuth2AccessTokenResponse("bearer", JsonWebToken.write(token2, secret), lifetime)
    complete(OAuth2AccessTokenResponseFormat.write(response))
  }

  private def completeWithToken(user: User): Route = {
    completeWithToken(JsonWebToken(
      createdAt = new Date(0),
      expiresAt = new Date(0),
      subject = user.id.stringify,
      claims = Map("name" -> JsString(user.username))
    ))
  }
}
