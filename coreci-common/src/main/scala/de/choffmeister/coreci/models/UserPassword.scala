package de.choffmeister.coreci.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._

import scala.concurrent._

case class UserPassword(
  id: BSONObjectID = BSONObjectID("00" * 12),
  userId: BSONObjectID,
  password: String = "",
  createdAt: BSONDateTime = BSONDateTime(0)) extends BaseModel

class UserPasswordTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[UserPassword](database, collection) {
  implicit val reader = UserPasswordBSONFormat.Reader
  implicit val writer = UserPasswordBSONFormat.Writer

  override def preInsert(obj: UserPassword): Future[UserPassword] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(obj.copy(id = id, createdAt = now))
  }

  def findCurrentPassword(userId: BSONObjectID): Future[Option[UserPassword]] = queryOne(BSONDocument(
    "$query" -> BSONDocument("userId" -> userId),
    "$orderby" -> BSONDocument("createdAt" -> -1)
  ))

  collection.indexesManager.ensure(Index(List("userId" -> IndexType.Ascending, "createdAt" -> IndexType.Descending)))
}

object UserPasswordBSONFormat {
  implicit object Reader extends BSONDocumentReader[UserPassword] {
    def read(doc: BSONDocument): UserPassword = UserPassword(
      id = doc.getAs[BSONObjectID]("_id").get,
      userId = doc.getAs[BSONObjectID]("userId").get,
      password = doc.getAs[String]("password").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[UserPassword] {
    def write(obj: UserPassword): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userId" -> obj.userId,
      "password" -> obj.password,
      "createdAt" -> obj.createdAt
    )
  }
}
