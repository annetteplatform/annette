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

package biz.lobachev.annette.gateway.core.authentication.keycloak

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.gateway.core.authentication.Subject
import play.api.libs.json._
import play.api.mvc.Headers

import scala.util.Try

trait KeycloakSubjectBuilder {

  def buildSubject(json: JsObject, headers: Headers, keycloakConf: KeycloakConfig): Subject = {
    val principals = keycloakConf.principals.flatMap {
      case PrincipalItem.Const(principalType, principalId)       =>
        Some(AnnettePrincipal(principalType, principalId))
      case PrincipalItem.Token(principalType, required, field)   =>
        getFieldValue(json, field, required).map(AnnettePrincipal(principalType, _))
      case PrincipalItem.Header(principalType, required, header) =>
        getHeaderValue(headers, header, required).map(AnnettePrincipal(principalType, _))
      case _                                                     => throw new RuntimeException(s"Principal configuration error")

    }
    val attributes = keycloakConf.attributes.toList.flatMap {
      case AttributeItem.Const(attribute, value)             => Some(attribute -> value)
      case AttributeItem.Token(attribute, required, field)   =>
        getFieldValue(json, field, required).map(attribute -> _)
      case AttributeItem.Header(attribute, required, header) =>
        getHeaderValue(headers, header, required).map(attribute -> _)
      case _                                                 => throw new RuntimeException(s"Attribute configuration error")

    }.toMap
    Subject(principals, attributes)
  }

  def getFieldValue(json: JsObject, field: String, required: Boolean): Option[String] =
    Try {
      val value: JsValue = (json \ field).get
      valueToString(value)

    }.recover { th =>
      if (required)
        throw th
      else
        None
    }.get

  def valueToString(value: JsValue): Option[String] =
    value match {
      case JsString(value)  => Some(value)
      case JsBoolean(true)  => Some("true")
      case JsBoolean(false) => Some("false")
      case JsNumber(value)  => Some(value.toString)
      case JsArray(items)   =>
        Some(items.map(valueToString).flatten.mkString(";"))
      case _                => throw new IllegalArgumentException("Value not found")
    }

  def getHeaderValue(headers: Headers, header: String, required: Boolean): Option[String] = {
    val res = headers.get(header)

    res match {
      case Some(value) => Some(value)
      case None        =>
        if (required)
          throw new RuntimeException(s"Required header $header not found in request")
        else
          None
    }
  }

}
