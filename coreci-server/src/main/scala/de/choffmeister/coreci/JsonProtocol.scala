package de.choffmeister.coreci

import java.util.Date

import akka.http.marshallers.sprayjson.SprayJsonSupport
import de.choffmeister.coreci.models._
import reactivemongo.bson._
import spray.json._

trait DateJsonProtocol extends DefaultJsonProtocol {
  implicit object DateFormat extends JsonFormat[Date] {
    def write(date: Date): JsValue = JsNumber(date.getTime)
    def read(value: JsValue): Date =
      value match {
        case JsNumber(dateTicks) => new Date(dateTicks.toLong)
        case _ => deserializationError(s"Date time ticks expected. Got '$value'")
      }
  }
}

trait BSONJsonProtocol extends DefaultJsonProtocol {
  implicit object BSONObjectIDFormat extends JsonFormat[BSONObjectID] {
    def write(id: BSONObjectID): JsValue = JsString(id.stringify)
    def read(value: JsValue): BSONObjectID =
      value match {
        case JsString(str) => BSONObjectID(str)
        case _ => deserializationError("BSON ID expected: " + value)
      }
  }

  implicit object BSONDateTimeFormat extends JsonFormat[BSONDateTime] {
    def write(dateTime: BSONDateTime): JsValue = JsNumber(dateTime.value)
    def read(value: JsValue): BSONDateTime =
      value match {
        case JsNumber(dateTimeTicks) => BSONDateTime(dateTimeTicks.toLong)
        case _ => deserializationError(s"Date time ticks expected. Got '$value'")
      }
  }
}

trait JsonProtocol extends DefaultJsonProtocol
    with DateJsonProtocol
    with BSONJsonProtocol
    with SprayJsonSupport {
  implicit object BuildStatusFormat extends JsonFormat[BuildStatus] {
    def write(status: BuildStatus): JsValue = status match {
      case Pending =>
        JsObject(
          "type" -> JsString("pending"))
      case Running(startedAt) =>
        JsObject(
          "type" -> JsString("running"),
          "startedAt" -> BSONDateTimeFormat.write(startedAt))
      case Succeeded(startedAt, finishedAt) =>
        JsObject(
          "type" ->JsString("succeeded"),
          "startedAt" -> BSONDateTimeFormat.write(startedAt),
          "finishedAt" -> BSONDateTimeFormat.write(finishedAt))
      case Failed(startedAt, finishedAt, errorMessage) =>
        JsObject(
          "type" -> JsString("failed"),
          "startedAt" -> BSONDateTimeFormat.write(startedAt),
          "finishedAt" -> BSONDateTimeFormat.write(finishedAt),
          "errorMessage" -> JsString(errorMessage))
    }

    def read(value: JsValue): BuildStatus =
      value match {
        case _ => deserializationError(s"Build status expected. Got '$value'")
      }
  }

  implicit val userFormat = jsonFormat5(User)
  implicit val jobFormat = jsonFormat5(Job)
  implicit val buildFormat = jsonFormat5(Build)
  implicit val outputFormat = jsonFormat5(Output)
}
