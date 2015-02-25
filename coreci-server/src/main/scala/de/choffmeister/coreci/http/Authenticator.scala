package de.choffmeister.coreci.http

import akka.http.model.headers._
import akka.http.server.directives.AuthenticationDirectives._
import de.choffmeister.auth.common.JsonWebToken
import de.choffmeister.auth.common.JsonWebToken._

import scala.concurrent._

class Authenticator[U](
    realm: String,
    bearerTokenSecret: Array[Byte],
    findUserById: String => Future[Option[U]],
    findUserByUserName: String => Future[Option[U]],
    validatePassword: (U, String) => Future[Boolean])(implicit executor: ExecutionContext) {
  def basic: AuthenticationDirective[U] = {
    authenticateOrRejectWithChallenge[BasicHttpCredentials, U] {
      case Some(BasicHttpCredentials(username, password)) =>
        findUserByUserName(username).flatMap {
          case Some(user) =>
            validatePassword(user, password).map {
              case true => grant(user)
              case false => deny
            }
          case None => Future(deny)

        }
      case None => Future(deny)
    }
  }

  def bearerToken(acceptExpired: Boolean = false): AuthenticationDirective[U] = {
    def resolve(token: JsonWebToken): Future[AuthenticationResult[U]] = findUserById(token.subject).map {
      case Some(user) => grant(user)
      case None => deny(None)
    }

    authenticateOrRejectWithChallenge[OAuth2BearerToken, U] {
      case Some(OAuth2BearerToken(tokenStr)) =>
        JsonWebToken.read(tokenStr, bearerTokenSecret) match {
          case Right(token) => resolve(token)
          case Left(Expired(token)) if acceptExpired => resolve(token)
          case Left(error) => Future(deny(Some(error)))
        }
      case None => Future(deny(Some(Missing)))
    }
  }

  private def grant(user: U) = AuthenticationResult.success(user)
  private def deny = AuthenticationResult.failWithChallenge(createBasicChallenge)
  private def deny(error: Option[Error]) = AuthenticationResult.failWithChallenge(createBearerTokenChallenge(error))

  private def createBasicChallenge: HttpChallenge = {
    HttpChallenge("Basic", realm)
  }

  private def createBearerTokenChallenge(error: Option[Error]): HttpChallenge = {
    val desc = error match {
      case None => None
      case Some(Missing) => None
      case Some(Malformed) => Some("The access token is malformed")
      case Some(InvalidSignature) => Some("The access token has been manipulated")
      case Some(Expired(_)) => Some("The access token expired")
      case _ => Some("An unknown error occured")
    }
    val params = desc match {
      case Some(msg) => Map("error" -> "invalid_token", "error_description" -> msg)
      case None => Map.empty[String, String]
    }
    HttpChallenge("Bearer", realm, params)
  }
}
