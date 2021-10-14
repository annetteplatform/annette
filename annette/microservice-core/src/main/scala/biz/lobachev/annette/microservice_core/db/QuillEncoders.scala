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

package biz.lobachev.annette.microservice_core.db

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import io.getquill.MappedEncoding
import play.api.libs.json.{Json, Reads, Writes}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.Date

trait QuillEncoders {
  implicit val offsetDataTimeEncoder: MappedEncoding[OffsetDateTime, Date] =
    MappedEncoding[OffsetDateTime, Date](odt => Date.from(odt.toInstant))

  implicit val offsetDataTimeDecoder: MappedEncoding[Date, OffsetDateTime] =
    MappedEncoding[Date, OffsetDateTime](_.toInstant.atOffset(ZoneOffset.UTC))

  implicit val principalEncoder: MappedEncoding[AnnettePrincipal, String] =
    MappedEncoding[AnnettePrincipal, String](_.code)

  implicit val principalDecoder: MappedEncoding[String, AnnettePrincipal] =
    MappedEncoding[String, AnnettePrincipal](AnnettePrincipal.fromCode)

  def genericJsonEncoder[T](implicit writes: Writes[T]): MappedEncoding[T, String] =
    MappedEncoding[T, String](t => Json.toJson(t).toString())

  def genericJsonDecoder[T](implicit reads: Reads[T]): MappedEncoding[String, T] =
    MappedEncoding[String, T](string => Json.parse(string).validate[T].get)

  def genericStringEncoder[T]: MappedEncoding[T, String] = MappedEncoding[T, String](_.toString)

  def genericStringDecoder[T](d: String => T): MappedEncoding[String, T] = MappedEncoding[String, T](d)

}
