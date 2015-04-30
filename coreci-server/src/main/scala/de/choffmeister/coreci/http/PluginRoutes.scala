package de.choffmeister.coreci.http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci.Plugin
import de.choffmeister.coreci.models.Database
import spray.json._

import scala.concurrent.ExecutionContext

class PluginRoutes(val database: Database, val plugins: Map[String, Plugin])
    (implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Routes {

  lazy val routes =
    pathEnd {
      get {
        complete {
          JsArray(plugins.toVector.map(_._2).map(p => pluginToJson(p)))
        }
      }
    } ~
    pathPrefix(Segment) { pluginName =>
      plugins.get(pluginName) match {
        case Some(plugin) =>
          pathEnd {
            get {
              complete {
                pluginToJson(plugin)
              }
            }
          } ~
          plugin.routes
        case None =>
          reject
      }
    }

  private def pluginToJson(plugin: Plugin) =
    JsObject(
      "name" -> JsString(plugin.name),
      "version" -> JsString(plugin.version)
    )
}
