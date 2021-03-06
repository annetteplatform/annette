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

package biz.lobachev.annette.core.model.auth

import play.api.libs.json.Json

import java.security.Principal

case class AnnettePrincipal(principalType: String, principalId: String) extends Principal {
  override def getName: String = code
  def code                     = s"$principalType~$principalId"
}

object AnnettePrincipal {
  implicit val format = Json.format[AnnettePrincipal]

  def fromCode(code: String): Option[AnnettePrincipal] = {
    val arr = code.split("~")
    if (arr.length == 2)
      Some(AnnettePrincipal(arr(0), arr(1)))
    else
      None
  }
}

trait SimplePrincipal {
  val PRINCIPAL_TYPE: String
  def apply(principalId: String): AnnettePrincipal         = AnnettePrincipal(PRINCIPAL_TYPE, principalId)
  def unapply(principal: AnnettePrincipal): Option[String] =
    principal match {
      case AnnettePrincipal(PRINCIPAL_TYPE, principalId) => Some(principalId)
      case _                                             => None
    }
}

object PersonPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "person"
}

object OrgPositionPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "org-position"
}

object OrgRolePrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "org-role"
}

object UnitChiefPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "unit-chief"
}

object DirectUnitPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "direct-unit"
}

object DescendantUnitPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "descendant-unit"
}

object TechnicalPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "tech"
}
