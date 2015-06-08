package de.choffmeister.coreci.plugins

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import de.choffmeister.coreci._
import org.apache.commons.codec.binary.Hex
import spray.json._

import scala.concurrent._

class GitHubPlugin(implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: FlowMaterializer) extends Plugin
    with Logger
    with DefaultJsonProtocol
    with SprayJsonSupport {
  val name = "github"
  val version = "0.0.1"

  def routes = {
    path("hook") {
      post {
        extractHook(Some("secret")) { case (delivery, event, payload) =>
          val repositoryFullName = payload.fields("repository").asJsObject.fields("full_name").asInstanceOf[JsString].value
          log.info(s"Received GitHub $event web hook ($delivery) for repository $repositoryFullName")

          event match {
            case "push" =>
              val shaAfter = payload.fields("after").asInstanceOf[JsString].value
              val archiveUrl = payload.fields("repository").asJsObject.fields("archive_url").asInstanceOf[JsString].value
                .replace("{archive_format}", "tarball")
                .replace("{/ref}", "/" + shaAfter)
              val future = rawRequest(GET, Uri(archiveUrl), maxRedirects = 0, failOnTooManyRedirects = false)
                .map(_.header[Location].get.uri)

              onSuccess(future)(realArchiveUrl => complete(realArchiveUrl.toString()))
            case e =>
              log.warn(s"Unsupportet GitHub $e web hook")
              reject()
          }
        }
      }
    }
  }

  private def extractHook(secret: Option[String]): Directive1[(String, String, JsObject)] = {
    val signatureRegex = "^([a-z0-9]+)=([0-9a-f]+)$".r

    def extract = headerValueByName("X-GitHub-Delivery").flatMap { delivery =>
      headerValueByName("X-GitHub-Event").flatMap { event =>
        entity(as[String]).map(s => JsonParser(ParserInput.apply(s)).asJsObject).flatMap { payload =>
          provide((delivery, event, payload))
        }
      }
    }

    optionalHeaderValueByName("X-Hub-Signature").flatMap { signature =>
      entity(as[ByteString]).flatMap { payloadBinary =>
        (secret, signature) match {
          case (None, _) =>
            extract
          case (Some(sec), Some(sig)) =>
            sig match {
              case signatureRegex("sha1", hex) =>
                if (checkHmacSHA1(hex, sec, payloadBinary)) extract
                else {
                  log.error("GitHub web hook has invalid signature")
                  reject(AuthorizationFailedRejection)
                }
              case signatureRegex(algo, _) =>
                log.error(s"GitHub web hook uses unsupported signature algorithm $algo")
                reject(AuthorizationFailedRejection)
              case _ =>
                log.error(s"GitHub web hook signature has invalid format")
                reject(AuthorizationFailedRejection)
            }
          case _ =>
            log.error(s"GitHub web hook lacks signature")
            reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  private def checkHmacSHA1(signatureHex: String, secret: String, data: ByteString) = {
    val dataArray = new Array[Byte](data.length)
    data.copyToArray(dataArray, 0, data.length)

    val algo = "HmacSHA1"
    val sig1 = Hex.decodeHex(signatureHex.toCharArray)
    val key = new SecretKeySpec(secret.getBytes("UTF-8"), algo)
    val mac = Mac.getInstance(algo)
    mac.init(key)
    val sig2 = mac.doFinal(dataArray)

    compareFixedTime(sig1, sig2)
  }

  private def compareFixedTime(a: Array[Byte], b: Array[Byte]) = {
    if (a.length == b.length) {
      var equal = true
      for (i <- a.indices) if (a(i) != b(i)) equal = false
      equal
    } else false
  }

  private def rawRequest(method: HttpMethod, uri: Uri, entity: RequestEntity = HttpEntity.Empty, maxRedirects: Int = 3, failOnTooManyRedirects: Boolean = true): Future[HttpResponse] = {
    val host = uri.authority.host.toString()
    val port = uri.authority.port
    val flow = uri.scheme match {
      case "http" if port != 0 => Http().outgoingConnection(host, 80)
      case "http" => Http().outgoingConnection(host, port)
      case "https" if port != 0 => Http().outgoingConnectionTls(host, port)
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

    Source.single(HttpRequest(method, uri, entity = entity)).via(flow).runWith(Sink.head)
      .flatMap { res =>
        val redirect = extractRedirect(res)
        redirect match {
          case Some(nextUri) if maxRedirects > 0 => rawRequest(method, nextUri, entity, maxRedirects - 1)
          case Some(nextUri) if failOnTooManyRedirects => Future.failed(new Exception("Exceeded maximum number of redirects"))
          case _ => Future(res)
        }
      }
  }
}
