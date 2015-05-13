package de.choffmeister.coreci.http

import akka.http.scaladsl.testkit._
import de.choffmeister.coreci.JsonProtocol

trait Specs2RouteTest extends TestFrameworkInterface with RouteTest with JsonProtocol {
  override def cleanUp(): Unit = {}

  override def failTest(msg: String): Nothing = {
    throw new Exception(msg)
  }
}
