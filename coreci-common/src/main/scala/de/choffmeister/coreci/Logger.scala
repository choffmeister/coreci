package de.choffmeister.coreci

import com.typesafe.scalalogging.{Logger => ScalaLogger}
import org.slf4j.{LoggerFactory => JavaLoggerFactory}

trait Logger { self =>
  lazy val log = ScalaLogger(JavaLoggerFactory.getLogger(self.getClass))
}
