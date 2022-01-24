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

package biz.lobachev.annette.bpm_repository.impl.schema

import biz.lobachev.annette.bpm_repository.api.domain.{BpmModelId, Code, Notation}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

trait BpmRepositorySchemaImplicits {
  /* WrappedString */
  implicit val bpmModelIdColumnType: JdbcType[BpmModelId] with BaseTypedType[BpmModelId] =
    MappedColumnType.base[BpmModelId, String](_.value, BpmModelId(_))
  implicit val codeColumnType: JdbcType[Code] with BaseTypedType[Code]                   =
    MappedColumnType.base[Code, String](_.value, Code(_))

  implicit val annettePrincipalColumnType: JdbcType[AnnettePrincipal] with BaseTypedType[AnnettePrincipal] =
    MappedColumnType.base[AnnettePrincipal, String](_.code, AnnettePrincipal.fromCode(_))

  /* Enumeration */
  implicit val notationColumnType: JdbcType[Notation.Notation] with BaseTypedType[Notation.Notation] =
    MappedColumnType.base[Notation.Notation, String](Notation.toDBRepresentation, Notation.fromDBRepresentation)

}
