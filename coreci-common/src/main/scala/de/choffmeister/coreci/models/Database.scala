package de.choffmeister.coreci.models

import akka.http.scaladsl.model.Uri
import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.core.commands.Drop

import scala.concurrent._

abstract class BaseModel {
  val id: BSONObjectID
}

abstract class Table[M <: BaseModel](database: Database, collection: BSONCollection)(implicit val executor: ExecutionContext) {
  implicit val reader: BSONDocumentReader[M]
  implicit val writer: BSONDocumentWriter[M]

  def all: Future[List[M]] = query()
  def find(id: BSONObjectID): Future[Option[M]] = queryOne(byId(id))
  def query(q: BSONDocument = BSONDocument.empty, sort: BSONDocument = BSONDocument.empty, page: (Option[Int], Option[Int]) = (None, None)): Future[List[M]] = {
    page match {
      case (Some(skip), Some(limit)) => collection.find(q).sort(sort).options(QueryOpts(skipN = skip, batchSizeN = limit)).cursor[M].collect[List](limit)
      case (Some(skip), None) => collection.find(q).sort(sort).options(QueryOpts(skipN = skip)).cursor[M].collect[List]()
      case (None, Some(limit)) => collection.find(q).options(QueryOpts(batchSizeN = limit)).sort(sort).cursor[M].collect[List](limit)
      case (None, None) => collection.find(q).sort(sort).cursor[M].collect[List]()
    }
  }
  def queryOne(q: BSONDocument): Future[Option[M]] = collection.find(q).one[M]
  def insert(m: M): Future[M] = preInsert(m).flatMap(m2 => preUpdate(m2).flatMap(m3 => collection.insert(m3).map(_ => m3)))
  def update(m: M): Future[M] = preUpdate(m).flatMap(m2 => collection.update(byId(m2.id), m2).map(_ => m2))
  def delete(id: BSONObjectID): Future[Unit] = collection.remove(byId(id)).map(_ => Unit)
  def delete(m: M): Future[Unit] = delete(m.id)

  def preInsert(m: M): Future[M] = Future.successful(m)
  def preUpdate(m: M): Future[M] = Future.successful(m)

  def configure(): Future[Unit] = {
    Future()
  }

  def clear(): Future[Unit] = {
    database.mongoDbDatabase.command(new Drop(collection.name))
      .map(_ => ())
      .recover { case _ => () }
  }

  private def byId(id: BSONObjectID): BSONDocument = BSONDocument("_id" -> id)
}

class Database(val mongoDbDatabase: DefaultDB, collectionNamePrefix: String = "")(implicit ec: ExecutionContext) {
  lazy val users = new UserTable(this, mongoDbDatabase(collectionNamePrefix + "users"))
  lazy val userPasswords = new UserPasswordTable(this, mongoDbDatabase(collectionNamePrefix + "userPasswords"))
  lazy val projects = new ProjectTable(this, mongoDbDatabase(collectionNamePrefix + "projects"))
  lazy val builds = new BuildTable(this, mongoDbDatabase(collectionNamePrefix + "builds"))
  lazy val outputs = new OutputTable(this, mongoDbDatabase(collectionNamePrefix + "outputs"))

  def configure(): Future[Unit] = Future.sequence(tables.map(_.configure())).map(_ => ())
  def clear(): Future[Unit] = Future.sequence(tables.map(_.clear())).map(_ => ())

  private lazy val tables = Seq(users, userPasswords, projects, builds, outputs)
}

object Database {
  lazy val mongoDbDriver = new MongoDriver

  def open(uri: String, databaseName: String, collectionNamePrefix: String = "")(implicit ec: ExecutionContext): Database = {
    val (host, port) = parseUri(uri)
    val mongoDbConnection = mongoDbDriver.connection(host + ":" + port :: Nil)
    val mongoDbDatabase = mongoDbConnection(databaseName)

    new Database(mongoDbDatabase, collectionNamePrefix)
  }

  private def parseUri(uri: String): (String, Int) = Uri(uri) match {
    case Uri("mongodb", authority, Uri.Path.Empty | Uri.Path.SingleSlash, Uri.Query.Empty, None) =>
      (authority.host.address(), if (authority.port > 0) authority.port else 27017)
    case _ =>
      throw new Exception(s"Unsupported URI '$uri' for MongoDB host")
  }
}
