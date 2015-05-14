package de.choffmeister.coreci.http

import akka.actor._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.Plugin
import de.choffmeister.coreci.models._
import reactivemongo.core.commands.LastError

import scala.concurrent.ExecutionContext

class ApiRoutes(val database: Database, workerHandler: ActorRef, val plugins: Map[String, Plugin])
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {
  lazy val authRoutes = new AuthRoutes(database)
  lazy val projectRoutes = new ProjectRoutes(database, workerHandler)
  lazy val buildRoutes = new BuildRoutes(database, workerHandler)
  lazy val workerRoutes = new WorkerRoutes(database, workerHandler)
  lazy val pluginRoutes = new PluginRoutes(database, plugins)

  lazy val routes = handleDatabaseExceptions {
    filterHttpChallengesByExtensionHeader {
      pathPrefix("auth")(authRoutes.routes) ~
      pathPrefix("projects")(projectRoutes.routes) ~
      pathPrefix("builds")(buildRoutes.routes) ~
      pathPrefix("workers")(workerRoutes.routes) ~
      pathPrefix("plugins")(pluginRoutes.routes)
    }
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

  def handleDatabaseExceptions: Directive0 = {
    val exceptionHandler = ExceptionHandler({
      case err @ LastError(_, _, Some(11000 | 11001), _, _, _, _) =>
        complete(UnprocessableEntity, err.getMessage)
    })

    handleExceptions(exceptionHandler)
  }
}
