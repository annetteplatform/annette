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

package biz.lobachev.annette.api_gateway_core.api.keycloak

import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticatedAction, MaybeAuthenticatedAction}
import biz.lobachev.annette.api_gateway_core.authentication.keycloak.KeycloakConfig
import biz.lobachev.annette.core.exception.AnnetteException
import biz.lobachev.annette.core.message.ErrorMessage

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext
@Singleton
class KeycloakController @Inject() (
  authenticated: AuthenticatedAction,
  maybeAuthenticated: MaybeAuthenticatedAction,
  maybeKeycloakConfig: Option[KeycloakConfig],
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def keycloak: Action[AnyContent] = keycloakApp("")

  def keycloakApp(application: String): Action[AnyContent] =
    Action { _ =>
      maybeKeycloakConfig.map { keycloakConfig =>
        val defaultConf     = keycloakConfig.config
        val applicationConf = if (application.trim.nonEmpty) keycloakConfig.applicationConfig.get(application) else None
        val conf            = applicationConf
          .map(appConf =>
            defaultConf.copy(
              realm = appConf.realm.getOrElse(defaultConf.realm),
              authServerUrl = appConf.authServerUrl.getOrElse(defaultConf.authServerUrl),
              publicAuthServerUrl = appConf.publicAuthServerUrl.getOrElse(defaultConf.publicAuthServerUrl),
              sslRequired = appConf.sslRequired.map(Some(_)).getOrElse(defaultConf.sslRequired),
              resource = appConf.resource.map(Some(_)).getOrElse(defaultConf.resource),
              publicClient = appConf.publicClient.map(Some(_)).getOrElse(defaultConf.publicClient)
            )
          )
          .getOrElse(defaultConf)
        val keycloakJson    = KeycloakJson(conf)
        Ok(Json.toJson(keycloakJson))
      }.getOrElse(NotFound)
    }

  def test =
    maybeAuthenticated { request =>
      val subject = request.subject.toString
      Ok(subject)
    }

  def error1 =
    authenticated { _ =>
      throw new AnnetteException(ErrorMessage("annette.exception"))
    }

  def error2 =
    authenticated { _ =>
      throw new RuntimeException("annette.runtimeException")
    }

}
