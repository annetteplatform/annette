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

import play.api.libs.json._

case class AnnettePrincipal(code: String) extends AnyVal {
  def principalType: String = split._1
  def principalId: String   = split._2
  def split: (String, String) = {
    val arr = code.split("~")
    if (arr.length == 2) arr(0) -> arr(1)
    else code   -> ""
  }
}

object AnnettePrincipal {
  implicit val format = Json.valueFormat[AnnettePrincipal]

  def apply(principalType: String, principalId: String): AnnettePrincipal =
    AnnettePrincipal(s"$principalType~$principalId")
}

trait SimplePrincipal {
  val PRINCIPAL_TYPE: String
  def apply(principalId: String): AnnettePrincipal         = AnnettePrincipal(PRINCIPAL_TYPE, principalId)
  def unapply(principal: AnnettePrincipal): Option[String] =
    if (principal.principalType == PRINCIPAL_TYPE) Some(principal.principalId)
    else None
}

object PersonPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "person" // pers
}

object OrgPositionPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "org-position" // opos
}

object OrgRolePrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "org-role" // orol
}

object UnitChiefPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "unit-chief" // ouch
}

object DirectUnitPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "direct-unit" // odru
}

object DescendantUnitPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "descendant-unit" // odsu
}

object TechnicalPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "tech" // tech
}

object PrincipalGroupPrincipal extends SimplePrincipal {
  val PRINCIPAL_TYPE = "principal-group" // prgr
}

object AuthenticatedPrincipal {
  val PRINCIPAL_TYPE: String                             = "authenticated" // auth
  val PRINCIPAL_ID: String                               = "user"
  def apply(): AnnettePrincipal                          = AnnettePrincipal(PRINCIPAL_TYPE, PRINCIPAL_ID)
  def unapply(principal: AnnettePrincipal): Option[Unit] =
    if (principal.principalType == PRINCIPAL_TYPE && principal.principalId == PRINCIPAL_ID) Some(())
    else None
}

object AnonymousPrincipal {
  val PRINCIPAL_TYPE: String                             = "person" // anon
  val PRINCIPAL_ID: String                               = "ANONYMOUS"
  def apply(): AnnettePrincipal                          = AnnettePrincipal(PRINCIPAL_TYPE, PRINCIPAL_ID)
  def unapply(principal: AnnettePrincipal): Option[Unit] =
    if (principal.principalType == PRINCIPAL_TYPE && principal.principalId == PRINCIPAL_ID) Some(())
    else None
}

object SystemPrincipal {
  val PRINCIPAL_TYPE: String                             = "tech"
  val PRINCIPAL_ID: String                               = "SYSTEM"
  def apply(): AnnettePrincipal                          = AnnettePrincipal(PRINCIPAL_TYPE, PRINCIPAL_ID)
  def unapply(principal: AnnettePrincipal): Option[Unit] =
    if (principal.principalType == PRINCIPAL_TYPE && principal.principalId == PRINCIPAL_ID) Some(())
    else None
}
