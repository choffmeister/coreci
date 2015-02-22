package de.choffmeister.coreci

import akka.actor._
import akka.http.Http
import akka.http.server.Directives._
import akka.stream.ActorFlowMaterializer
import de.choffmeister.coreci.http._
import de.choffmeister.coreci.models._

import scala.concurrent.duration._

class Server extends Bootable with JsonProtocol {
  implicit val system = ActorSystem("coreci")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()
  lazy val config = Config.load()
  lazy val database = Database.open(config.mongoDbServers, config.mongoDbDatabaseName)

  TestDataGenerator.generate(config, database)

  def startup(args: List[String]): Unit = {
    val binding = Http(system).bind(interface = config.httpInterface, port = config.httpPort)
    val apiRoutes = new ApiRoutes(database)
    val staticContentRoutes = new StaticContentRoutes(config.webDir)
    val routes =
      pathPrefix("api")(apiRoutes.routes) ~
      pathPrefixTest(!"api")(staticContentRoutes.routes)
    binding.startHandlingWith(compressResponseIfRequested()(routes))
  }

  def shutdown(): Unit = {
    system.shutdown()
    system.awaitTermination(1.seconds)
  }
}

object Server extends BootableApp[Server]
