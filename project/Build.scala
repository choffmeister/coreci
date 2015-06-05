import sbt._
import sbt.Keys._
import xerial.sbt.Pack.{ pack => sbtPack, _ }
import de.choffmeister.sbt.WebAppPlugin.{ npmBuild => sbtNpmBuild, _ }
import com.typesafe.sbt.{ GitVersioning => sbtGit }
import com.typesafe.sbt.SbtGit.git

object Build extends sbt.Build {
  lazy val dist = TaskKey[File]("dist", "Builds the distribution packages")

  lazy val buildSettings = Seq(
    scalaVersion := "2.11.5",
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val coordinateSettings = Seq(
    organization := "de.choffmeister")

  lazy val resolverSettings = Seq(
    resolvers ++= Seq(
      "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"))

  lazy val commonSettings = Defaults.coreDefaultSettings ++ coordinateSettings ++ buildSettings ++
    resolverSettings ++ Seq(libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.3.1" % "test"))

  lazy val serverPackSettings = packSettings ++ Seq(
    packMain := Map("coreci" -> "de.choffmeister.coreci.Application"),
    packExtraClasspath := Map("coreci" -> Seq("${PROG_HOME}/conf")))

  lazy val common = (project in file("coreci-common"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      "com.typesafe" % "config" % "1.2.0",
      "com.typesafe.akka" %% "akka-actor" % "2.3.10",
      "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-RC2",
      "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC2",
      "com.typesafe.akka" %% "akka-slf4j" % "2.3.10",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.10" % "test",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "de.choffmeister" %% "auth-common" % "0.0.2",
      "io.spray" %% "spray-json" % "1.3.1",
      "org.apache.commons" % "commons-compress" % "1.9",
      "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.2",
      "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"))
    .settings(serverPackSettings: _*)
    .settings(name := "coreci-common")

  lazy val server = (project in file("coreci-server"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-scala-experimental" % "1.0-RC2",
      "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "1.0-RC2",
      "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % "1.0-RC2" % "test",
      "org.rogach" %% "scallop" % "0.9.5"))
    .settings(serverPackSettings: _*)
    .settings(name := "coreci-server")
    .dependsOn(common % "test->test;compile->compile")

  lazy val web = (project in file("coreci-web"))
    .settings(commonSettings: _*)
    .settings(webAppSettings: _*)
    .settings(name := "coreci-web")

  lazy val root = (project in file("."))
    .settings(coordinateSettings: _*)
    .settings(name := "coreci")
    .settings(dist <<= (streams, target, sbtPack in server, sbtNpmBuild in web) map { (s, target, server, web) =>
      val distDir = target / "dist"
      val distBinDir = distDir / "bin"
      val distWebDir = distDir / "web"
      IO.copyDirectory(server, distDir)
      IO.copyDirectory(web / "build", distWebDir)
      distBinDir.listFiles.foreach(_.setExecutable(true, false))
      distDir
    })
    .enablePlugins(sbtGit)
    .settings(git.formattedShaVersion := git.gitHeadCommit.value map(sha => s"${sha.take(7)}-SNAPSHOT"))
    .aggregate(common, server, web)
}
