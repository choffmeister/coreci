package de.choffmeister.coreci

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.choffmeister.coreci.models._
import reactivemongo.bson._
import spray.json._

import scala.concurrent.duration._

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

trait FiniteDurationProtocol extends DefaultJsonProtocol {
  implicit object FiniteDurationFormat extends JsonFormat[FiniteDuration] {
    def write(dur: FiniteDuration): JsValue = JsNumber(dur.toMillis)
    def read(value: JsValue): FiniteDuration =
      value match {
        case JsNumber(millis) => FiniteDuration(millis.toLong, TimeUnit.MILLISECONDS)
        case _ => deserializationError(s"Finite duration milliseconds expected. Got '$value'")
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
    with FiniteDurationProtocol
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
  implicit val environmentVariableFormat = jsonFormat3(EnvironmentVariable)
  implicit val projectFormat = jsonFormat11(Project)
  implicit val buildFormat = jsonFormat10(Build)
  implicit val outputFormat = jsonFormat5(Output)
  implicit val dockerVersionFormat = jsonFormat4(DockerVersion)
  implicit val dockerHostInfoFormat = jsonFormat5(DockerHostInfo)
  implicit val workerFormat = jsonFormat9(Worker)
}
