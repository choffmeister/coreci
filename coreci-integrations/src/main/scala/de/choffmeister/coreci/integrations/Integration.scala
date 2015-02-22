package de.choffmeister.coreci.integrations

import akka.http.server.Route

trait Integration {
  val name: String
  val routes: Route
}
