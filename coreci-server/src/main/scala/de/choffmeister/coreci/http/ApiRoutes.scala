package de.choffmeister.coreci.http

import akka.actor._
import akka.http.model._
import akka.http.model.headers._
import akka.http.server.Directives._
import akka.http.server._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.models._

import scala.concurrent.ExecutionContext

class ApiRoutes(val database: Database)
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val authRoutes = new AuthRoutes(database)
  lazy val projectRoutes = new ProjectRoutes(database)
  lazy val buildRoutes = new BuildRoutes(database)

  lazy val routes = filterHttpChallengesByExtensionHeader {
    pathPrefix("auth")(authRoutes.routes) ~
    pathPrefix("projects")(projectRoutes.routes) ~
    pathPrefix("builds")(buildRoutes.routes)
  }

  def filterHttpChallengesByExtensionHeader: Directive0 =
    extract(ctx => ctx.request.headers).flatMap { headers =>
      headers.find(_.lowercaseName == "x-www-authenticate-filter") match {
        case Some(HttpHeader(_, value)) =>
          val filter = value.split(" ").filter(_ != "").map(_.toLowerCase).toSeq
          filterHttpChallenges(c => filter.contains(c.scheme.toLowerCase))
        case _ =>
          pass
      }
    }

  def filterHttpChallenges(cond: HttpChallenge => Boolean): Directive0 = mapRejections { rejections =>
    rejections.filter {
      case AuthenticationFailedRejection(c, ch) => cond(ch)
      case rejection => true
    }
  }
}
