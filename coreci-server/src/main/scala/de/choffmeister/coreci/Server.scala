package de.choffmeister.coreci

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Flow
import de.choffmeister.coreci.http._
import de.choffmeister.coreci.models._

import scala.concurrent.duration._

class Server(config: Config, serverConfig: ServerConfig, database: Database) extends Bootable {
  implicit val system = ActorSystem("coreci")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  def startup(): Unit = {
    val binding = Http(system).bind(interface = serverConfig.httpInterface, port = serverConfig.httpPort)
    val apiRoutes = new ApiRoutes(database)
    val staticContentRoutes = new StaticContentRoutes(serverConfig.webDir)
    val routes =
      pathPrefix("api")(apiRoutes.routes) ~
      pathPrefixTest(!"api")(staticContentRoutes.routes)

    // TODO make parallelism configurable
    binding.runForeach(_.handleWith(Flow[HttpRequest].mapAsync(32, Route.asyncHandler(routes))))
  }

  def shutdown(): Unit = {
    system.shutdown()
    system.awaitTermination(3.seconds)
  }
}

trait Bootable {
  def startup(): Unit
  def shutdown(): Unit

  sys.ShutdownHookThread(shutdown())
}
