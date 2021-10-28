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

import pureconfig.generic.FieldCoproductHint

case class KeycloakConfig(
  config: RealmConfig,
  applicationConfig: Map[String, ClientConfig] = Map.empty,
  principals: Seq[PrincipalItem] = Seq.empty,
  attributes: Seq[AttributeItem] = Seq.empty
)

case class RealmConfig(
  realm: String,
  authServerUrl: String,
  sslRequired: Option[String],
  resource: Option[String],
  publicClient: Option[Boolean]
)

case class ClientConfig(
  realm: Option[String],
  authServerUrl: Option[String],
  sslRequired: Option[String],
  resource: Option[String],
  publicClient: Option[Boolean]
)

sealed trait PrincipalItem

object PrincipalItem {

  implicit val hint = new FieldCoproductHint[PrincipalItem]("source")

  case class Const(
    `type`: String,
    value: String
  ) extends PrincipalItem

  case class Token(
    `type`: String,
    required: Boolean = false,
    field: String
  ) extends PrincipalItem

  case class Header(
    `type`: String,
    required: Boolean = false,
    header: String
  ) extends PrincipalItem

}

sealed trait AttributeItem

object AttributeItem {

  implicit val hint = new FieldCoproductHint[AttributeItem]("source")

  case class Const(
    name: String,
    value: String
  ) extends AttributeItem

  case class Token(
    name: String,
    required: Boolean = false,
    field: String
  ) extends AttributeItem

  case class Header(
    name: String,
    required: Boolean = false,
    header: String
  ) extends AttributeItem

}
