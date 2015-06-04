package de.choffmeister.coreci.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import reactivemongo.core.commands.{FindAndModify, Update}

import scala.concurrent._

sealed trait BuildStatus
case object Pending extends BuildStatus
case class Running(startedAt: BSONDateTime) extends BuildStatus
case class Succeeded(startedAt: BSONDateTime, finishedAt: BSONDateTime) extends BuildStatus
case class Failed(startedAt: BSONDateTime, finishedAt: BSONDateTime, errorMessage: String) extends BuildStatus

case class Build(
  id: BSONObjectID = BSONObjectID("00" * 12),
  projectId: BSONObjectID,
  number: Int = 0,
  status: BuildStatus = Pending,
  image: String,
  script: String,
  environment: List[EnvironmentVariable] = Nil,
  output: String = "",
  createdAt: BSONDateTime = BSONDateTime(0),
  updatedAt: BSONDateTime = BSONDateTime(0),
  projectCanonicalName: String = "") extends BaseModel
{
  def defused: Build = this.copy(environment = this.environment.map(_.defused))
}

class BuildTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Build](database, collection) {
  implicit val reader = BuildJSONFormat.Reader
  implicit val writer = BuildJSONFormat.Writer

  override def preInsert(obj: Build): Future[Build] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    database.projects.getNextBuildNumber(obj.projectId).flatMap {
      case Some(number) =>
        database.projects.find(obj.projectId).map {
          case Some(project) =>
            obj.copy(id = id, number = number, createdAt = now, projectCanonicalName = project.canonicalName)
          case None =>
            ???
        }
      case None =>
        throw new Exception(s"Could not get build number because project ${obj.projectId} is unknown")
    }
  }

  override def preUpdate(obj: Build): Future[Build] = {
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(updatedAt = now))
  }

  override def configure(): Future[Unit] = {
    collection.indexesManager.ensure(Index(List("projectId" -> IndexType.Ascending, "number" -> IndexType.Descending), unique = true)).map(_ => ())
  }

  def list(page: (Option[Int], Option[Int])): Future[List[Build]] =
    query(sort = BSONDocument("createdAt" -> -1), page = page)
  def listByProject(projectId: BSONObjectID, page: (Option[Int], Option[Int])): Future[List[Build]] =
    query(BSONDocument("projectId" -> projectId), sort = BSONDocument("createdAt" -> -1), page = page)
  def findByNumber(projectId: BSONObjectID, number: Int): Future[Option[Build]] =
    queryOne(BSONDocument("projectId" -> projectId, "number" -> number))

  def getPending(): Future[Option[Build]] = {
    val now = BSONDateTime(System.currentTimeMillis)
    val selector = BSONDocument("status" -> BuildJSONFormat.StatusWriter.write(Pending))
    val modifier = Update(BSONDocument("$set" -> BSONDocument("status" -> BuildJSONFormat.StatusWriter.write(Running(now)))), fetchNewObject = true)

    database.mongoDbDatabase.command(new FindAndModify(collection.name, selector, modifier))
      .map(_.map(BuildJSONFormat.Reader.read))
  }
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
          doc.getAs[String]("errorMessage").get)
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
      case Failed(startedAt, finishedAt, errorMessage) =>
        BSONDocument(
          "type" -> "failed",
          "startedAt" -> startedAt,
          "finishedAt" -> finishedAt,
          "errorMessage" -> errorMessage)
    }
  }

  implicit val environmentVariableReader = EnvironmentVariableBSONFormat.Reader
  implicit val environmentVariableWriter = EnvironmentVariableBSONFormat.Writer

  implicit object Reader extends BSONDocumentReader[Build] {
    def read(doc: BSONDocument): Build = Build(
      id = doc.getAs[BSONObjectID]("_id").get,
      projectId = doc.getAs[BSONObjectID]("projectId").get,
      number = doc.getAs[Int]("number").get,
      status = doc.getAs[BuildStatus]("status").get,
      image = doc.getAs[String]("image").get,
      script = doc.getAs[String]("script").get,
      environment = doc.getAs[List[EnvironmentVariable]]("environment").get,
      output = doc.getAs[String]("output").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      updatedAt = doc.getAs[BSONDateTime]("updatedAt").get,
      projectCanonicalName = doc.getAs[String]("projectCanonicalName").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[Build] {
    def write(obj: Build): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "projectId" -> obj.projectId,
      "number" -> obj.number,
      "status" -> obj.status,
      "image" -> obj.image,
      "script" -> obj.script,
      "environment" -> obj.environment,
      "output" -> obj.output,
      "createdAt" -> obj.createdAt,
      "updatedAt" -> obj.updatedAt,
      "projectCanonicalName" -> obj.projectCanonicalName
    )
  }
}
