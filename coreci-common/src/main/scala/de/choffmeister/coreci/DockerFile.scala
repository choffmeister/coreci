package de.choffmeister.coreci

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}

case class DockerfileInstruction(name: String, arguments: String)

case class Dockerfile(instructions: Seq[DockerfileInstruction]) {
  def from(name: String): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("FROM", name))

  def comment(text: String): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("#", text))

  def workdir(path: String): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("WORKDIR", path))

  def run(command: String): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("RUN", command))

  def expose(port: Int): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("EXPOSE", port.toString))

  def cmd(command: String): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("CMD", command))

  def cmd(commandParts: List[String]): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("CMD", "[" + commandParts.map("\"" + _ + "\"").mkString(",") + "]"))

  def add(source: String, target: String): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("ADD", source + " " + target))

  def asString: String =
    instructions.map(inst => inst.name.toUpperCase + " " + inst.arguments).mkString("\n\n") + "\n"
}

object Dockerfile {
  def empty: Dockerfile = Dockerfile(Seq.empty)

  def from(name: String): Dockerfile = Dockerfile.empty.from(name)

  def parse(raw: String): Dockerfile = {
    def dropFirst(s: String): String = s.substring(1)
    def dropLast(s: String): String = s.substring(0, s.length - 1)

    val lines = raw.split("\n", -1).map(_.trim).foldLeft((List.empty[String], List.empty[String])) {
      case ((list, Nil), line) if line.endsWith("\\") =>
        (list, dropLast(line).trim :: Nil)
      case ((list, prelines), line) if line.endsWith("\\") =>
        (list, prelines :+ dropLast(line).trim)
      case ((list, Nil), line) =>
        (list :+ line.trim, Nil)
      case ((list, prelines), line) =>
        (list :+ (prelines :+ line.trim).mkString(" "), Nil)
    } match {
      case (list, Nil) => list
      case (list, lastline) => list :+ lastline.mkString(" ")
    }

    val instructions = lines.filter(_.length > 0).map {
      case comment if comment.startsWith("#") =>
        DockerfileInstruction("#", dropFirst(comment).trim)
      case instruction =>
        val inst = instruction.split(" ", 2).head.toUpperCase
        val args = instruction.split(" ", 2).drop(1).headOption
        DockerfileInstruction(inst, args.map(_.trim).getOrElse(""))
    }
    Dockerfile(instructions)
  }

  def createTarBall(dockerfile: Dockerfile, context: Map[String, ByteString] = Map.empty): Source[ByteString, Unit] = {
    val mem = new ByteArrayOutputStream()
    val tar = new TarArchiveOutputStream(new GZIPOutputStream(mem))

    val dockerfileEntry = new TarArchiveEntry("Dockerfile")
    val dockerfileContent = dockerfile.asString.getBytes("UTF-8")
    dockerfileEntry.setSize(dockerfileContent.length)
    tar.putArchiveEntry(dockerfileEntry)
    tar.write(dockerfileContent)
    tar.closeArchiveEntry()

    for (file <- context) {
      val fileEntry = new TarArchiveEntry(file._1)
      val fileContent = new Array[Byte](file._2.length)
      file._2.copyToArray(fileContent)
      fileEntry.setSize(fileContent.length)
      tar.putArchiveEntry(fileEntry)
      tar.write(fileContent)
      tar.closeArchiveEntry()
    }

    tar.close()

    Source(ByteString(mem.toByteArray) :: Nil)
  }
}
