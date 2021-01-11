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

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PublicKeyRequestor {
  val log = LoggerFactory.getLogger(this.getClass)
  sealed trait Message

  sealed trait Response             extends Message
  final case class Key(key: String) extends Response
  case object NoKey                 extends Response

  sealed trait Command                        extends Message
  case class Get(replyTo: ActorRef[Response]) extends Command
  case object RequestKey                      extends Command
  case object RequestAgain                    extends Command
  final case class SetKey(key: String)        extends Command

  def apply(realmConfig: RealmConfig, ws: WSClient, ec: ExecutionContext): Behavior[Command] =
    Behaviors.setup { context =>
      context.self ! RequestKey
      processMessages(None, realmConfig, ws, ec)
    }

  def processMessages(
    maybeKey: Option[String] = None,
    realmConfig: RealmConfig,
    ws: WSClient,
    ec: ExecutionContext
  ): Behavior[Command] =
    Behaviors.receive {
      case (context, RequestKey)   =>
        requestKey(context, realmConfig, ws)(ec)
        Behaviors.same
      case (context, RequestAgain) =>
        context.scheduleOnce(20.seconds, context.self, RequestKey)
        Behaviors.same
      case (_, SetKey(key))        =>
        processMessages(Some(key), realmConfig, ws, ec)
      case (_, Get(replyTo))       =>
        val response = maybeKey.map(Key).getOrElse(NoKey)
        replyTo ! response
        Behaviors.same

    }

  def requestKey(context: ActorContext[PublicKeyRequestor.Command], realmConfig: RealmConfig, ws: WSClient)(implicit
    ec: ExecutionContext
  ) = {
    val url       = s"${realmConfig.authServerUrl}/realms/${realmConfig.realm}"
    log.debug("Request public key from {}", url)
    val keyFuture = ws
      .url(url)
      .get()
      .map { response =>
        (response.json \ "public_key").as[String]
      }
    keyFuture.foreach { publicKey =>
      log.debug("Received Keycloak's public key {}", publicKey)
      context.self ! SetKey(publicKey)
    }

    keyFuture.failed.foreach { th =>
      log.error("Failed to get Keycloak's public key", th)
      context.self ! RequestAgain
    }

  }

}
