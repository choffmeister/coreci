package de.choffmeister.coreci

import javax.net.ssl.SSLContext

import akka.actor._
import akka.http.scaladsl.{HttpsContext, Http}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import spray.json._

import scala.collection.immutable
import scala.concurrent._
import scala.concurrent.duration._

object HttpClient {
  def request(method: HttpMethod, uri: Uri, entity: RequestEntity = HttpEntity.Empty, headers: immutable.Seq[HttpHeader] = Nil, followRedirects: Option[Int] = None)(implicit system: ActorSystem, mat: FlowMaterializer, ec: ExecutionContext): Future[HttpResponse] = {
    val host = uri.authority.host.toString()
    val port = uri.authority.port
    val flow = uri.scheme match {
      case "http" if port != 0 => Http().outgoingConnection(host, port)
      case "http" => Http().outgoingConnection(host, 80)
      case "https" if port != 0 => Http().outgoingConnectionTls(host, port, httpsContext = Some(HttpsContext(SSLContext.getDefault)))
      case "https" => Http().outgoingConnectionTls(host, 443)
    }

    def extractRedirect(res: HttpResponse): Option[Uri] = {
      if (res.status == MovedPermanently || res.status == Found) {
        res.header[Location] match {
          case Some(h) => Some(h.uri)
          case _ => throw new Exception("Redirection response without location header")
        }
      } else None
    }

    Source.single(HttpRequest(method, uri, entity = entity, headers = headers)).via(flow).runWith(Sink.head).flatMap { res =>
      val redirect = extractRedirect(res)
      (redirect, followRedirects) match {
        case (Some(nextUri), Some(mr)) if mr > 0 => request(method, nextUri, entity, headers, Some(mr - 1))
        case (Some(nextUri), Some(_)) => Future.failed(new Exception("Exceeded maximum number of redirects"))
        case _ => Future(res)
      }
    }
  }

  def jsonRequest(method: HttpMethod, uri: Uri, payload: Option[JsObject] = None)(implicit system: ActorSystem, mat: FlowMaterializer, ec: ExecutionContext): Future[(HttpResponse, Either[String, JsValue])] = {
    def readResponseBody(res: HttpResponse): Future[Either[String, JsValue]] = {
      res.entity.toStrict(3.second).map { body =>
        if (body.contentType == ContentTypes.`application/json`) Right(JsonParser(ParserInput(body.data.utf8String)))
        else Left(body.data.utf8String)
      }
    }

    val entity = payload match {
      case Some(p) => HttpEntity(p.toString()).withContentType(ContentTypes.`application/json`)
      case None => HttpEntity.Empty
    }

    for {
      res <- request(method, uri, entity)
      body <- readResponseBody(res)
    } yield (res, body)
  }
}
