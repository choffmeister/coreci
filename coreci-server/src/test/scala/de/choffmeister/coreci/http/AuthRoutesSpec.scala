package de.choffmeister.coreci.http

import akka.http.scaladsl.model.headers.{OAuth2BearerToken, BasicHttpCredentials, HttpChallenge}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsMissing
import de.choffmeister.auth.common._
import de.choffmeister.coreci._
import de.choffmeister.coreci.models._
import org.specs2.mutable._

class AuthRoutesSpec extends Specification with Specs2RouteTest {
  implicit val oauth2AccessTokenResponseFormat = OAuth2AccessTokenResponseFormat

  "AuthRoutes" should {
    "GET /auth/state without credentials" in new TestActorSystem {
      TestDatabase(prefill = false) { db =>
        val routes = new ApiRoutes(db, self, Map.empty).routes

        Get("/auth/state") ~> routes ~> check {
          rejections === List(
            AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", "coreci")),
            AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Basic", "coreci")))
        }

        Get("/auth/state") ~> addHeader("X-WWW-Authenticate-Filter", "Bearer") ~> routes ~> check {
          rejection === AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", "coreci"))
        }

        Get("/auth/state") ~> addHeader("X-WWW-Authenticate-Filter", "Basic") ~> routes ~> check {
          rejection === AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Basic", "coreci"))
        }
      }
    }

    "GET /auth/state valid basic credentials" in new TestActorSystem {
      TestDatabase(prefill = true) { db =>
        val routes = new ApiRoutes(db, self, Map.empty).routes
        val users = await(db.users.all)

        Get("/auth/state") ~> addCredentials(BasicHttpCredentials("user1", "pass1")) ~> routes ~> check {
          responseAs[User] === users.find(_.username == "user1").get
        }

        Get("/auth/state") ~> addCredentials(BasicHttpCredentials("user2", "pass2")) ~> routes ~> check {
          responseAs[User] === users.find(_.username == "user2").get
        }
      }
    }

    "GET /auth/state invalid basic credentials" in new TestActorSystem {
      TestDatabase(prefill = true) { db =>
        val routes = new ApiRoutes(db, self, Map.empty).routes

        Get("/auth/state") ~> addCredentials(BasicHttpCredentials("user1", "pass2")) ~> routes ~> check {
          rejections must not beEmpty
        }

        Get("/auth/state") ~> addCredentials(BasicHttpCredentials("user0", "pass0")) ~> routes ~> check {
          rejections must not beEmpty
        }
      }
    }

    "GET /auth/token/create"  in new TestActorSystem {
      TestDatabase(prefill = true) { db =>
        val routes = new ApiRoutes(db, self, Map.empty).routes
        val users = await(db.users.all)

        Get("/auth/token/create") ~> addCredentials(BasicHttpCredentials("user1", "pass1")) ~> routes ~> check {
          val res = responseAs[OAuth2AccessTokenResponse]

          Get("/auth/state") ~> addCredentials(OAuth2BearerToken(res.accessToken)) ~> routes ~> check {
            responseAs[User] === users.find(_.username == "user1").get
          }
        }

        Post("/auth/token/create") ~> addCredentials(BasicHttpCredentials("user0", "pass0")) ~> routes ~> check {
          rejections must not beEmpty
        }
      }
    }

    "GET /auth/token/renew"  in new TestActorSystem {
      TestDatabase(prefill = true) { db =>
        val routes = new ApiRoutes(db, self, Map.empty).routes
        val config = ServerConfig.load()

        Get("/auth/token/create") ~> addCredentials(BasicHttpCredentials("user1", "pass1")) ~> routes ~> check {
          val res1 = responseAs[OAuth2AccessTokenResponse]
          val token1 = JsonWebToken.read(res1.accessToken, config.authBearerTokenSecret).right.get

          Thread.sleep(1100L)

          Get("/auth/token/renew") ~> addCredentials(OAuth2BearerToken(res1.accessToken)) ~> routes ~> check {
            val res2 = responseAs[OAuth2AccessTokenResponse]
            val token2 = JsonWebToken.read(res2.accessToken, config.authBearerTokenSecret).right.get

            token1.expiresAt.getTime() must beLessThan(token2.expiresAt.getTime())
          }
        }
      }
    }
  }
}
