package de.choffmeister.coreci.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._

import scala.concurrent._

case class Job(
  id: BSONObjectID = BSONObjectID("00" * 12),
  userId: BSONObjectID,
  displayName: String,
  description: String,
  dockerfile: String,
  createdAt: BSONDateTime = BSONDateTime(0),
  updatedAt: BSONDateTime = BSONDateTime(0)) extends BaseModel

class JobTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Job](database, collection) {
  implicit val reader = JobBSONFormat.Reader
  implicit val writer = JobBSONFormat.Writer

  override def preInsert(obj: Job): Future[Job] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(id = id, createdAt = now))
  }

  override def preUpdate(obj: Job): Future[Job] = {
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(updatedAt = now))
  }

  collection.indexesManager.ensure(Index(List("userId" -> IndexType.Ascending)))
}

object JobBSONFormat {
  implicit object Reader extends BSONDocumentReader[Job] {
    def read(doc: BSONDocument): Job = Job(
      id = doc.getAs[BSONObjectID]("_id").get,
      userId = doc.getAs[BSONObjectID]("userId").get,
      displayName = doc.getAs[String]("displayName").get,
      description = doc.getAs[String]("description").get,
      dockerfile = doc.getAs[String]("dockerfile").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      updatedAt = doc.getAs[BSONDateTime]("updatedAt").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[Job] {
    def write(obj: Job): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userId" -> obj.userId,
      "displayName" -> obj.displayName,
      "description" -> obj.description,
      "dockerfile" -> obj.dockerfile,
      "createdAt" -> obj.createdAt,
      "updatedAt" -> obj.updatedAt
    )
  }
}
