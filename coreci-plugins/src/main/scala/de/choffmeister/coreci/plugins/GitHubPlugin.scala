package de.choffmeister.coreci.plugins

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
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
        extractHook(None) { case (delivery, event, payload) =>
          val repositoryFullName = payload.fields("repository").asJsObject.fields("full_name").asInstanceOf[JsString].value
          val shaAfter = payload.fields("after").asInstanceOf[JsString].value

          log.info(s"Received GitHub $event web hook ($delivery) for repository $repositoryFullName#$shaAfter")

          complete("ok")
        }
      }
    }
  }

  private def extractHook(secret: Option[String]): Directive1[(String, String, JsObject)] = {
    val signatureRegex = "^(sha1)=([0-9a-f]+)$".r

    def extract = headerValueByName("X-GitHub-Delivery").flatMap { delivery =>
      headerValueByName("X-GitHub-Event").flatMap { event =>
        entity(as[JsObject]).flatMap { payload =>
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
                  reject
                }
              case signatureRegex(algo, _) =>
                log.error(s"GitHub web hook uses unsupported signature algorithm $algo")
                reject
              case _ =>
                log.error(s"GitHub web hook signature has invalid format")
                reject
            }
          case _ =>
            log.error(s"GitHub web hook lacks signature")
            reject
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
      for (i <- 0 until a.length) if (a(i) != b(i)) equal = false
      equal
    } else false
  }
}
