package de.choffmeister.coreci

import org.specs2.mutable.Specification

class DockerfileSpec extends Specification {
  "Dockerfile" should {
    "split raw files into instructions" in {
      val raw = """|#This is a comment
                   |FROM ubuntu
                   |
                   |RUN \
                   |  foo &&   \
                   |    bar
                   |
                   |# This is another comment
                   |unknown   instruction
                   |open foo \
                 """.stripMargin

      val dockerfile = Dockerfile.parse(raw)
      dockerfile.instructions(0) === DockerfileInstruction("#", "This is a comment")
      dockerfile.instructions(1) === DockerfileInstruction("FROM", "ubuntu")
      dockerfile.instructions(2) === DockerfileInstruction("RUN", "foo && bar")
      dockerfile.instructions(3) === DockerfileInstruction("#", "This is another comment")
      dockerfile.instructions(4) === DockerfileInstruction("UNKNOWN", "instruction")
      dockerfile.instructions(5) === DockerfileInstruction("OPEN", "foo")
    }
  }
}
