package de.choffmeister.coreci.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import reactivemongo.core.commands.{FindAndModify, Update}

import scala.concurrent._

case class Project(
  id: BSONObjectID = BSONObjectID("00" * 12),
  userId: BSONObjectID,
  canonicalName: String,
  title: String,
  description: String,
  dockerfile: String,
  nextBuildNumber: Int = 1,
  createdAt: BSONDateTime = BSONDateTime(0),
  updatedAt: BSONDateTime = BSONDateTime(0)) extends BaseModel

class ProjectTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Project](database, collection) {
  implicit val reader = ProjectBSONFormat.Reader
  implicit val writer = ProjectBSONFormat.Writer

  override def preInsert(obj: Project): Future[Project] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(id = id, createdAt = now))
  }

  override def preUpdate(obj: Project): Future[Project] = {
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(updatedAt = now))
  }

  override def configure(): Future[Unit] = {
    collection.indexesManager.ensure(Index(List("userId" -> IndexType.Ascending))).map(_ => ())
  }

  def findByCanonicalName(cn: String): Future[Option[Project]] = queryOne(BSONDocument("canonicalName" -> cn))

  def getNextBuildNumber(projectId: BSONObjectID): Future[Option[Int]] = {
    val selector = BSONDocument("_id" -> projectId)
    val modifier = Update(BSONDocument("$inc" -> BSONDocument("nextBuildNumber" -> 1)), fetchNewObject = false)

    database.mongoDbDatabase.command(new FindAndModify(collection.name, selector, modifier)).map {
      case Some(doc) =>
        val buildNumber = doc.getAs[Int]("nextBuildNumber").get
        Some(buildNumber)
      case None =>
        None
    }
  }
}

object ProjectBSONFormat {
  implicit object Reader extends BSONDocumentReader[Project] {
    def read(doc: BSONDocument): Project = Project(
      id = doc.getAs[BSONObjectID]("_id").get,
      userId = doc.getAs[BSONObjectID]("userId").get,
      canonicalName = doc.getAs[String]("canonicalName").get,
      title = doc.getAs[String]("title").get,
      description = doc.getAs[String]("description").get,
      dockerfile = doc.getAs[String]("dockerfile").get,
      nextBuildNumber = doc.getAs[Int]("nextBuildNumber").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      updatedAt = doc.getAs[BSONDateTime]("updatedAt").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[Project] {
    def write(obj: Project): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userId" -> obj.userId,
      "canonicalName" -> obj.canonicalName,
      "title" -> obj.title,
      "description" -> obj.description,
      "dockerfile" -> obj.dockerfile,
      "nextBuildNumber" -> obj.nextBuildNumber,
      "createdAt" -> obj.createdAt,
      "updatedAt" -> obj.updatedAt
    )
  }
}
