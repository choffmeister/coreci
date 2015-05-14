package de.choffmeister.coreci.models

import reactivemongo.bson._

case class EnvironmentVariable(
  name: String,
  value: String,
  secret: Boolean)
{
  def defused: EnvironmentVariable = {
    if (secret) this.copy(value = "***")
    else this
  }
}

object EnvironmentVariableBSONFormat {
  implicit object Reader extends BSONDocumentReader[EnvironmentVariable] {
    def read(doc: BSONDocument): EnvironmentVariable = EnvironmentVariable(
      name = doc.getAs[String]("name").get,
      value = doc.getAs[String]("value").get,
      secret = doc.getAs[Boolean]("secret").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[EnvironmentVariable] {
    def write(obj: EnvironmentVariable): BSONDocument = BSONDocument(
      "name" -> obj.name,
      "value" -> obj.value,
      "secret" -> obj.secret
    )
  }
}
