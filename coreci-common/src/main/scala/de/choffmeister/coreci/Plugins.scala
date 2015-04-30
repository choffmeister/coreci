package de.choffmeister.coreci

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.FlowMaterializer

import scala.concurrent.ExecutionContext

trait Plugin {
  val name: String
  val version: String

  def routes(implicit executor: ExecutionContext, materializer: FlowMaterializer): Route
}

object Plugins {
  def init(config: Config): Map[String, Plugin] = {
    val ps = config.pluginClassNames.map { cn =>
      val clazz = getClass.getClassLoader.loadClass(cn).asSubclass(classOf[Plugin])
      clazz.newInstance()
    }

    (ps.map(_.name) zip ps).toMap
  }
}
