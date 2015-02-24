package de.choffmeister.coreci.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._

import scala.concurrent._

case class Output(
  id: BSONObjectID = BSONObjectID("00" * 12),
  buildId: BSONObjectID,
  content: String,
  timestamp: BSONDateTime = BSONDateTime(0)) extends BaseModel

class OutputTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Output](database, collection) {
  implicit val reader = OutputBSONFormat.Reader
  implicit val writer = OutputBSONFormat.Writer

  override def preInsert(obj: Output): Future[Output] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(id = id, timestamp = now))
  }

  collection.indexesManager.ensure(Index(List("buildId" -> IndexType.Ascending, "timestamp" -> IndexType.Ascending)))
}

object OutputBSONFormat {
  implicit object Reader extends BSONDocumentReader[Output] {
    def read(doc: BSONDocument): Output = Output(
      id = doc.getAs[BSONObjectID]("_id").get,
      buildId = doc.getAs[BSONObjectID]("buildId").get,
      content = doc.getAs[String]("content").get,
      timestamp = doc.getAs[BSONDateTime]("timestamp").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[Output] {
    def write(obj: Output): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "buildId" -> obj.buildId,
      "content" -> obj.content,
      "timestamp" -> obj.timestamp
    )
  }
}
