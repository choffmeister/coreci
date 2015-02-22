package de.choffmeister.coreci.http

import akka.http.model.Uri.Path
import akka.http.server.PathMatcher.{ Matched, Unmatched, Matching }
import akka.http.server.PathMatcher1
import reactivemongo.bson.BSONObjectID

import scala.util.Success

object BSONObjectIDSegment extends PathMatcher1[BSONObjectID] {
  def apply(path: Path): Matching[Tuple1[BSONObjectID]] = path match {
    case Path.Segment(segment, tail) =>
      BSONObjectID.parse(segment) match {
        case Success(id) => Matched(tail, Tuple1(id))
        case _ => Unmatched
      }
    case _ => Unmatched
  }
}
