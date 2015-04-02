package de.choffmeister.coreci

import akka.actor._
import akka.http.Http
import akka.http.server.Directives._
import akka.stream.ActorFlowMaterializer
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
    binding.startHandlingWith(compressResponseIfRequested()(routes))
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
