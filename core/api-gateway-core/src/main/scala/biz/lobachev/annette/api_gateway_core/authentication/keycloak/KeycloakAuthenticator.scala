/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.api_gateway_core.authentication.keycloak

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.util.Timeout
import biz.lobachev.annette.api_gateway_core.authentication.{
  AuthenticationFailedException,
  BearerAuthenticator,
  Subject,
  TokenExpiredException
}
import biz.lobachev.annette.core.exception.AnnetteException

import org.slf4j.LoggerFactory
import pdi.jwt.exceptions.JwtExpirationException
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import play.api.mvc.Headers

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class KeycloakAuthenticator(keycloakConfOpt: Option[KeycloakConfig], ws: WSClient, system: ActorSystem)(implicit
  val ec: ExecutionContext
) extends BearerAuthenticator
    with KeycloakSubjectBuilder {

  val log = LoggerFactory.getLogger(this.getClass)

  val keycloakRequestorActorOpt: Option[ActorRef[PublicKeyRequestor.Command]] = createRequestorActor

  override def authenticate[A](headers: Headers, token: String)(implicit ec: ExecutionContext): Future[Subject] =
    keycloakConfOpt.map { keycloakConf =>
      authenticate(token, headers, keycloakConf).recover {
        case aex: AnnetteException => throw aex
        case th: Throwable         =>
          log.error("Authentication failed", th)
          throw AuthenticationFailedException()
      }
    }.getOrElse {
      log.error("Authentication failed. Keycloak config failure")
      Future.failed(AuthenticationFailedException())
    }

  private def authenticate(token: String, headers: Headers, keycloakConf: KeycloakConfig)(implicit
    ec: ExecutionContext
  ): Future[Subject] =
    for {
      publicKey <- getPublicKey
    } yield {
      val json = decodeToken(publicKey, token)
      buildSubject(json, headers, keycloakConf)
    }

  private def getPublicKey: Future[String] = {
    implicit val timeout       = Timeout(50.seconds)
    implicit val scheduler     = system.toTyped.scheduler
    val keycloakRequestorActor = keycloakRequestorActorOpt.get
    keycloakRequestorActor.ask[PublicKeyRequestor.Response](ref => PublicKeyRequestor.Get(ref)).map {
      case PublicKeyRequestor.Key(key) => key
      case PublicKeyRequestor.NoKey    => throw AuthenticationFailedException()
    }
  }

  private def decodeToken(publicKey: String, token: String): JsObject =
    JwtJson.decodeJson(token, publicKey, Seq(JwtAlgorithm.RS256)) match {
      case Success(json)                      => json
      case Failure(_: JwtExpirationException) => throw TokenExpiredException()
      case Failure(_: Throwable)              => throw AuthenticationFailedException()
    }

  import akka.actor.typed.scaladsl.adapter._

  private def createRequestorActor: Option[ActorRef[PublicKeyRequestor.Command]] =
    keycloakConfOpt.map { keycloakConf =>
      system.spawn[PublicKeyRequestor.Command](
        PublicKeyRequestor(keycloakConf.config, ws, ec),
        "keycloakPublicKeyRequestor"
      )
    }

}
