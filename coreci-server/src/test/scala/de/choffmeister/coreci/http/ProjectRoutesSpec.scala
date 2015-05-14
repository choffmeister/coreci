package de.choffmeister.coreci.http

import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsMissing
import de.choffmeister.coreci._
import de.choffmeister.coreci.models._
import org.specs2.mutable._
import reactivemongo.bson.BSONObjectID

class ProjectRoutesSpec extends Specification with Specs2RouteTest {
  "ProjectRoutes" should {
    "GET /projects" in new TestActorSystem {
      TestDatabase(prefill = true) { db =>
        val routes = new ApiRoutes(db, self).routes
        val projects = await(db.projects.list(page = (None, None)))

        Get("/projects") ~> routes ~> check {
          responseAs[List[Project]] === projects.map(_.defused)
        }

        Get("/projects?skip=0&limit=1") ~> routes ~> check {
          responseAs[List[Project]] === List(projects(0)).map(_.defused)
        }

        Get("/projects?skip=1&limit=1") ~> routes ~> check {
          responseAs[List[Project]] === List(projects(1)).map(_.defused)
        }
      }
    }

    "GET /projects/{projectCanonicalName}" in new TestActorSystem {
      TestDatabase(prefill = true) { db =>
        val routes = new ApiRoutes(db, self).routes
        val projects = await(db.projects.list(page = (None, None)))

        Get("/projects/unknown-project") ~> routes ~> check {
          rejections must beEmpty
        }

        Get(s"/projects/${projects(0).canonicalName}") ~> routes ~> check {
          responseAs[Project] === projects(0).defused
        }

        Get(s"/projects/${projects(1).canonicalName}") ~> routes ~> check {
          responseAs[Project] === projects(1).defused
        }
      }
    }

    "POST /projects" in new TestActorSystem {
      TestDatabase(prefill = true) { db =>
        val routes = new ApiRoutes(db, self).routes
        val newProject = Project(
          userId = BSONObjectID.generate,
          canonicalName = "new-project",
          title = "New Project",
          description = "",
          image = "busybox:latest",
          script = ""
        )

        Post("/projects") ~> routes ~> check {
          rejections === List(
            AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", "coreci")),
            AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Basic", "coreci")))
        }

        Post("/projects", newProject) ~> addCredentials(BasicHttpCredentials("user1", "pass1")) ~> routes ~> check {
          responseAs[Project].canonicalName == newProject.canonicalName
        }
      }
    }
  }
}
