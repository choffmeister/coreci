package de.choffmeister.coreci.http

import akka.http.scaladsl.testkit._
import de.choffmeister.coreci.JsonProtocol

import scala.concurrent.duration._

trait Specs2RouteTest extends TestFrameworkInterface with RouteTest with JsonProtocol {
  implicit val timeout = RouteTestTimeout(5.seconds)

  override def cleanUp(): Unit = {}

  override def failTest(msg: String): Nothing = {
    throw new Exception(msg)
  }
}
