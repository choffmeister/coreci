package de.choffmeister.coreci

import de.choffmeister.coreci.models.Database
import org.rogach.scallop._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.language.reflectiveCalls

object Application extends App with Logger {
  val cla = new CommandLineArguments(args.toList)

  cla.subcommand match {
    case Some(cla.reset) if cla.reset.testdata.get == Some(true) =>
      val config = Config.load()
      val database = Database.open(config.mongoDbServers, config.mongoDbDatabaseName)
      val generator = new TestDataGenerator(config, database)
      waitAndExit(for {
        _ <- database.clear()
        _ <- database.configure()
        _ <- generator.run()
      } yield ())
    case Some(cla.reset) =>
      val config = Config.load()
      val database = Database.open(config.mongoDbServers, config.mongoDbDatabaseName)
      waitAndExit(for {
        _ <- database.clear()
        _ <- database.configure()
      } yield ())
    case _ =>
      val config = Config.load()
      val serverConfig = ServerConfig.load()
      val database = Database.open(config.mongoDbServers, config.mongoDbDatabaseName)
      val server = new Server(config, serverConfig, database)
      server.startup()
  }

  private def waitAndExit[T](f: Future[T]) = {
    import scala.util._

    f.onComplete {
      case Success(_) =>
        System.exit(0)
      case Failure(ex) =>
        log.error("Error", ex)
        System.exit(1)
    }
  }
}

class CommandLineArguments(val arguments: List[String]) extends ScallopConf(arguments) {
  version("coreci (c) 2015 Christian Hoffmeister <mail@choffmeister.de>")

  val reset = new Subcommand("reset") {
    val testdata = opt("testdata", 't',
      default = Some(false),
      descr = "Fill database with test data")
  }
}
