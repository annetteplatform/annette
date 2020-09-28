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

package biz.lobachev.annette.gateway.core.authentication.basic
import biz.lobachev.annette.core.model.AnnettePrincipal
import biz.lobachev.annette.gateway.core.authentication.Subject
import play.api.mvc.Headers

trait BasicAuthSubjectBuilder {

  def buildSubject(headers: Headers, basicAuthAccount: BasicAuthAccount): Subject = {
    val principals = basicAuthAccount.principals.flatMap {
      case PrincipalItem.Const(principalType, principalId)       =>
        Some(AnnettePrincipal(principalType, principalId))
      case PrincipalItem.Header(principalType, required, header) =>
        getHeaderValue(headers, header, required).map(AnnettePrincipal(principalType, _))
      case _                                                     => throw new RuntimeException(s"Principal configuration error")
    }
    val attributes = basicAuthAccount.attributes.toList.flatMap {
      case AttributeItem.Const(attribute, value)             =>
        Some(attribute -> value)
      case AttributeItem.Header(attribute, required, header) =>
        getHeaderValue(headers, header, required).map(attribute -> _)
      case _                                                 => throw new RuntimeException(s"Attribute configuration error")

    }.toMap
    Subject(principals, attributes)
  }

  def getHeaderValue(headers: Headers, header: String, required: Boolean): Option[String] =
    headers.get(header) match {
      case Some(value) => Some(value)
      case None        =>
        if (required)
          throw new RuntimeException(s"Required header $header not found in request")
        else
          None
    }

}
