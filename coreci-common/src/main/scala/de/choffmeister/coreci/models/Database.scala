package de.choffmeister.coreci.models

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

  def all: Future[List[M]] = query(BSONDocument.empty)
  def find(id: BSONObjectID): Future[Option[M]] = queryOne(byId(id))
  def query(q: BSONDocument): Future[List[M]] = collection.find(q).cursor[M].collect[List]()
  def queryOne(q: BSONDocument): Future[Option[M]] = collection.find(q).one[M]
  def insert(m: M): Future[M] = preInsert(m).flatMap(m2 => preUpdate(m2).flatMap(m3 => collection.insert(m3).map(_ => m3)))
  def update(m: M): Future[M] = preUpdate(m).flatMap(m2 => collection.update(byId(m2.id), m2).map(_ => m2))
  def delete(id: BSONObjectID): Future[Unit] = collection.remove(byId(id)).map(_ => Unit)
  def delete(m: M): Future[Unit] = delete(m.id)

  def preInsert(m: M): Future[M] = Future.successful(m)
  def preUpdate(m: M): Future[M] = Future.successful(m)

  private def byId(id: BSONObjectID): BSONDocument = BSONDocument("_id" -> id)
}

class Database(val mongoDbDatabase: DefaultDB, collectionNamePrefix: String = "")(implicit ec: ExecutionContext) {
  lazy val users = new UserTable(this, mongoDbDatabase(collectionNamePrefix + "users"))
  lazy val userPasswords = new UserPasswordTable(this, mongoDbDatabase(collectionNamePrefix + "userPasswords"))
  lazy val projects = new ProjectTable(this, mongoDbDatabase(collectionNamePrefix + "projects"))
  lazy val builds = new BuildTable(this, mongoDbDatabase(collectionNamePrefix + "builds"))
  lazy val outputs = new OutputTable(this, mongoDbDatabase(collectionNamePrefix + "outputs"))

  def clear(): Future[Unit] =
    Future.sequence(Seq(
      mongoDbDatabase.command(new Drop(collectionNamePrefix + "users")),
      mongoDbDatabase.command(new Drop(collectionNamePrefix + "userPasswords")),
      mongoDbDatabase.command(new Drop(collectionNamePrefix + "projects")),
      mongoDbDatabase.command(new Drop(collectionNamePrefix + "builds")),
      mongoDbDatabase.command(new Drop(collectionNamePrefix + "outputs"))
    )).map(_ => ())
}

object Database {
  lazy val mongoDbDriver = new MongoDriver

  def open(servers: List[(String, Int)], databaseName: String, collectionNamePrefix: String = "")(implicit ec: ExecutionContext): Database = {
    val mongoDbConnection = mongoDbDriver.connection(servers.map(x => x._1 + ":" + x._2.toString))
    val mongoDbDatabase = mongoDbConnection(databaseName)

    new Database(mongoDbDatabase, collectionNamePrefix)
  }
}
