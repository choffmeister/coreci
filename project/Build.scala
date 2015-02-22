import sbt._
import sbt.Keys._
import xerial.sbt.Pack.{ pack => sbtPack, _ }
import de.choffmeister.sbt.WebAppPlugin.{ webAppBuild => sbtWebAppBuild, _ }

object Build extends sbt.Build {
  lazy val dist = TaskKey[File]("dist", "Builds the distribution packages")

  lazy val buildSettings = Seq(
    scalaVersion := "2.11.5",
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val coordinateSettings = Seq(
    organization := "de.choffmeister",
    version := "0.0.1-SNAPSHOT")

  lazy val resolverSettings = Seq(
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/")

  lazy val commonSettings = Defaults.coreDefaultSettings ++ coordinateSettings ++ buildSettings ++ resolverSettings

  lazy val serverPackSettings = packSettings ++ Seq(
    packMain := Map("server" -> "de.choffmeister.coreci.Server"),
    packExtraClasspath := Map("server" -> Seq("${PROG_HOME}/conf")))

  lazy val server = (project in file("coreci-server"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "com.typesafe" % "config" % "1.2.0",
      "com.typesafe.akka" %% "akka-actor" % "2.3.7",
      "com.typesafe.akka" %% "akka-http-experimental" % "1.0-M3",
      "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "1.0-M3",
      "com.typesafe.akka" %% "akka-slf4j" % "2.3.7",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.7" % "test",
      "de.choffmeister" %% "auth-common" % "0.0.1",
      "org.almoehi" %% "reactive-docker" % "0.1-SNAPSHOT",
      "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
      "org.specs2" %% "specs2" % "2.4.1" % "test"))
    .settings(serverPackSettings: _*)
    .settings(name := "coreci-server")

  lazy val web = (project in file("coreci-web"))
    .settings(commonSettings: _*)
    .settings(webAppSettings: _*)
    .settings(name := "coreci-web")

  lazy val root = (project in file("."))
    .settings(coordinateSettings: _*)
    .settings(name := "coreci")
    .settings(dist <<= (streams, target, sbtPack in server, sbtWebAppBuild in web) map { (s, target, server, web) =>
      val distDir = target / "dist"
      val distBinDir = distDir / "bin"
      val distWebDir = distDir / "web"
      IO.copyDirectory(server, distDir)
      IO.copyDirectory(web, distWebDir)
      distBinDir.listFiles.foreach(_.setExecutable(true, false))
      distDir
    })
    .aggregate(server, web)
}
