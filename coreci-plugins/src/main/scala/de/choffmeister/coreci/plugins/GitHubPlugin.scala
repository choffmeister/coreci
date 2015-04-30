package de.choffmeister.coreci.plugins

import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import de.choffmeister.coreci._

import scala.concurrent.ExecutionContext

class GitHubPlugin extends Plugin {
  val name = "github"
  val version = "0.0.1"

  def routes(implicit executor: ExecutionContext, materializer: FlowMaterializer) =
    path("foo") {
      complete("bar")
    }
}
