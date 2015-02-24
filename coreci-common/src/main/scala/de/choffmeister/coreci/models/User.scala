package de.choffmeister.coreci.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._

import scala.concurrent._

case class User(
  id: BSONObjectID = BSONObjectID("00" * 12),
  username: String,
  email: String,
  createdAt: BSONDateTime = BSONDateTime(0),
  updatedAt: BSONDateTime = BSONDateTime(0)) extends BaseModel

class UserTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[User](database, collection) {
  implicit val reader = UserBSONFormat.Reader
  implicit val writer = UserBSONFormat.Writer

  override def preInsert(obj: User): Future[User] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(id = id, createdAt = now))
  }

  override def preUpdate(obj: User): Future[User] = {
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(updatedAt = now))
  }

  def findByUserName(userName: String): Future[Option[User]] = queryOne(BSONDocument("username" -> userName))

  collection.indexesManager.ensure(Index(List("username" -> IndexType.Ascending), unique = true))
  collection.indexesManager.ensure(Index(List("email" -> IndexType.Ascending), unique = true))
}

object UserBSONFormat {
  implicit object Reader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument): User = User(
      id = doc.getAs[BSONObjectID]("_id").get,
      username = doc.getAs[String]("username").get,
      email = doc.getAs[String]("email").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      updatedAt = doc.getAs[BSONDateTime]("updatedAt").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[User] {
    def write(obj: User): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "username" -> obj.username,
      "email" -> obj.email,
      "createdAt" -> obj.createdAt,
      "updatedAt" -> obj.updatedAt
    )
  }
}
