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

package biz.lobachev.annette.gateway.api.auth

import biz.lobachev.annette.core.exception.AnnetteException
import biz.lobachev.annette.core.message.ErrorMessage
import biz.lobachev.annette.gateway.core.authentication.AuthenticatedAction
import biz.lobachev.annette.gateway.core.authentication.keycloak.KeycloakConfig
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext
@Singleton
class KeycloakController @Inject() (
  authenticated: AuthenticatedAction,
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
    authenticated { request =>
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
