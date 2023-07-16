/*
 * Copyright 2018 Valery Lobachev
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

package biz.lobachev.annette.ignition.keycloak

import biz.lobachev.annette.ignition.keycloak.loaders.data.UserData
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

class KeycloakService(
  ws: WSClient,
  server: KeycloakConfig,
  targetRealm: String,
  defaultPassword: String,
  temporaryPassword: Boolean,
  idAttribute: String
)(implicit ec: ExecutionContext) {

  var token: Future[String] = login()

  def login() =
    ws.url(s"${server.url}/realms/${server.realm}/protocol/openid-connect/token")
      .addHttpHeaders("content-type" -> "application/x-www-form-urlencoded")
      .post(
        Map(
          "grant_type" -> "password",
          "client_id"  -> server.clientId,
          "username"   -> server.username,
          "password"   -> server.password
        )
      )
      .map { response =>
        (response.json \ "access_token").as[String]
      }

  def registerUser(user: UserData) =
    for {
      tk <- token
      _  <- createUser(tk, user).recoverWith {
              case AuthorizationFailed() =>
                token = login()
                for {
                  tk <- token
                  _  <- createUser(tk, user)
                } yield true
              case th                    => Future.failed(th)
            }
    } yield ()

  private def createUser(token: String, reg: UserData)(implicit ec: ExecutionContext) = {

    val data = Json.obj(
      "username"    -> reg.username.getOrElse(s"${reg.firstname}.${reg.lastname}").toString,
      "lastName"    -> reg.lastname,
      "firstName"   -> reg.firstname,
      "email"       -> reg.email.getOrElse(null),
      "emailVerified" -> reg.email.map(_ => JsTrue).getOrElse(null),
      "enabled"     -> true,
      "attributes"  -> Json.obj(
        s"$idAttribute" -> reg.id
      ),
      "credentials" -> Json.arr(
        Json.obj(
          "type"      -> "password",
          "value"     -> reg.password.getOrElse(defaultPassword).toString,
          "temporary" -> temporaryPassword
        )
      )
    )

    ws.url(s"${server.url}/admin/realms/$targetRealm/users")
      .addHttpHeaders(
        "Authorization" -> s"Bearer $token"
      )
      .post(data)
      .map { response =>
        response.status match {
          case 201 => true
          case 401 =>
            throw AuthorizationFailed()
          case 409 =>
            throw UserAlreadyRegistered()
          case _   =>
            throw new RuntimeException(response.statusText)
        }
      }
  }

  case class AuthorizationFailed()   extends RuntimeException("authorization failed")
  case class UserAlreadyRegistered() extends RuntimeException("user already registered")

}
