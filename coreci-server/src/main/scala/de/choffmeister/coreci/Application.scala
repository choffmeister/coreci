package de.choffmeister.coreci

import com.typesafe.config.ConfigRenderOptions
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
      val database = Database.open(config.mongoDbServer, config.mongoDbDatabaseName)
      val generator = new TestDataGenerator(config, database)
      waitAndExit(for {
        _ <- database.clear()
        _ <- database.configure()
        _ <- generator.run()
      } yield ())
    case Some(cla.reset) =>
      val config = Config.load()
      val database = Database.open(config.mongoDbServer, config.mongoDbDatabaseName)
      waitAndExit(for {
        _ <- database.clear()
        _ <- database.configure()
      } yield ())
    case Some(cla.config) =>
      val config = Config.load()
      val renderOpts = ConfigRenderOptions.defaults()
        .setOriginComments(cla.config.withOriginComments.get.getOrElse(false))
        .setComments(cla.config.withComments.get.getOrElse(false))
        .setJson(cla.config.json.get.getOrElse(false))
      println(config.raw.root().render(renderOpts))
    case _ =>
      val config = Config.load()
      val serverConfig = ServerConfig.load()
      val database = Database.open(config.mongoDbServer, config.mongoDbDatabaseName)
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

  val config = new Subcommand("config") {
    val json = opt("json",
      default = Some(false),
      descr = "Print in json format",
      noshort = true)

    val withComments = opt("with-comments",
      default = Some(false),
      descr = "Add comments",
      noshort = true)

    val withOriginComments = opt("with-origin-comments",
      default = Some(false),
      descr = "Add value origin comments",
      noshort = true)
  }
}
