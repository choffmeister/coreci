package de.choffmeister.coreci

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}

case class DockerfileInstruction(name: String, arguments: String)

case class Dockerfile(instructions: Seq[DockerfileInstruction]) {
  def from(repository: String, tag: Option[String]): Dockerfile =
    tag match {
      case Some(t) => copy(instructions = instructions :+ DockerfileInstruction("FROM", s"$repository:$t"))
      case None => copy(instructions = instructions :+ DockerfileInstruction("FROM", repository))
    }

  def run(command: String): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("RUN", command))

  def expose(port: Int): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("EXPOSE", port.toString))

  def cmd(command: String): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("CMD", command))

  def cmd(commandParts: List[String]): Dockerfile =
    copy(instructions = instructions :+ DockerfileInstruction("CMD", "[" + commandParts.map("\"" + _ + "\"").mkString(",") + "]"))

  def asString: String =
    instructions.map(inst => inst.name.toUpperCase + " " + inst.arguments).mkString("\n\n") + "\n"

  def asTar: Source[ByteString] = {
    val mem = new ByteArrayOutputStream()
    val tar = new TarArchiveOutputStream(new GZIPOutputStream(mem))
    val content = asString.getBytes("UTF-8")

    val entry = new TarArchiveEntry("Dockerfile")
    entry.setSize(content.length)
    tar.putArchiveEntry(entry)
    tar.write(content)
    tar.closeArchiveEntry()
    tar.close()

    Source(ByteString(mem.toByteArray) :: Nil)
  }
}

object Dockerfile {
  def empty: Dockerfile = Dockerfile(Seq.empty)

  def from(repository: String, tag: Option[String]): Dockerfile = Dockerfile.empty.from(repository, tag)
}
