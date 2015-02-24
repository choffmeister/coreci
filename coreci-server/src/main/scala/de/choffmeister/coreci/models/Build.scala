package de.choffmeister.coreci.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._

import scala.concurrent._

sealed trait BuildStatus
case object Pending extends BuildStatus
case class Running(startedAt: BSONDateTime) extends BuildStatus
case class Succeeded(startedAt: BSONDateTime, finishedAt: BSONDateTime) extends BuildStatus
case class Failed(startedAt: BSONDateTime, finishedAt: BSONDateTime, exitCode: Int) extends BuildStatus

case class Build(
  id: BSONObjectID = BSONObjectID("00" * 12),
  jobId: BSONObjectID,
  status: BuildStatus = Pending,
  createdAt: BSONDateTime = BSONDateTime(0),
  updatedAt: BSONDateTime = BSONDateTime(0)) extends BaseModel

class BuildTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Build](database, collection) {
  implicit val reader = BuildJSONFormat.Reader
  implicit val writer = BuildJSONFormat.Writer

  override def preInsert(obj: Build): Future[Build] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(id = id, createdAt = now))
  }

  override def preUpdate(obj: Build): Future[Build] = {
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(updatedAt = now))
  }

  collection.indexesManager.ensure(Index(List("jobId" -> IndexType.Ascending)))
}

object BuildJSONFormat {
  implicit object StatusReader extends BSONDocumentReader[BuildStatus] {
    def read(doc: BSONDocument): BuildStatus = doc.getAs[String]("type").get match {
      case "pending" =>
        Pending
      case "running" =>
        Running(
          doc.getAs[BSONDateTime]("startedAt").get)
      case "succeeded" =>
        Succeeded(
          doc.getAs[BSONDateTime]("startedAt").get,
          doc.getAs[BSONDateTime]("finishedAt").get)
      case "failed" =>
        Failed(
          doc.getAs[BSONDateTime]("startedAt").get,
          doc.getAs[BSONDateTime]("finishedAt").get,
          doc.getAs[Int]("exitCode").get)
      case _ =>
        ???
    }
  }

  implicit object StatusWriter extends BSONDocumentWriter[BuildStatus] {
    def write(obj: BuildStatus): BSONDocument = obj match {
      case Pending =>
        BSONDocument(
          "type" -> "pending")
      case Running(startedAt) =>
        BSONDocument(
          "type" -> "running",
          "startedAt" -> startedAt)
      case Succeeded(startedAt, finishedAt) =>
        BSONDocument(
          "type" -> "succeeded",
          "startedAt" -> startedAt,
          "finishedAt" -> finishedAt)
      case Failed(startedAt, finishedAt, exitCode) =>
        BSONDocument(
          "type" -> "failed",
          "startedAt" -> startedAt,
          "finishedAt" -> finishedAt,
          "exitCode" -> exitCode)
    }
  }

  implicit object Reader extends BSONDocumentReader[Build] {
    def read(doc: BSONDocument): Build = Build(
      id = doc.getAs[BSONObjectID]("_id").get,
      jobId = doc.getAs[BSONObjectID]("jobId").get,
      status = doc.getAs[BuildStatus]("status").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      updatedAt = doc.getAs[BSONDateTime]("updatedAt").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[Build] {
    def write(obj: Build): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "jobId" -> obj.jobId,
      "status" -> obj.status,
      "createdAt" -> obj.createdAt,
      "updatedAt" -> obj.updatedAt
    )
  }
}
