package de.choffmeister.coreci

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.FlowMaterializer

import scala.concurrent.ExecutionContext

abstract class Plugin(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer) {
  val name: String
  val version: String

  def routes: Route
}

object Plugins extends Logger {
  def init(config: Config)(implicit system: ActorSystem, executor: ExecutionContext, materializer: FlowMaterializer): Map[String, Plugin] = {
    val plugins =
      config.pluginClassNames.map { cn =>
        log.info(s"Initializing plugin $cn")
        try {
          val clazz = getClass.getClassLoader.loadClass(cn).asSubclass(classOf[Plugin])
          val ctor = clazz.getDeclaredConstructor(classOf[ActorSystem], classOf[ExecutionContext], classOf[FlowMaterializer])
          Some(ctor.newInstance(system, executor, materializer))
        } catch  {
          case ex: Exception =>
            log.error(s"Error while initializing plugin $cn", ex)
            None
        }
      }
      .filter(_.isDefined)
      .map(_.get)

    (plugins.map(_.name) zip plugins).toMap
  }
}
