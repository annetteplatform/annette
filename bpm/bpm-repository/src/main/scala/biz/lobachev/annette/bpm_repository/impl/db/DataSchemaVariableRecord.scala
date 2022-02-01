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

package biz.lobachev.annette.bpm_repository.impl.db

import biz.lobachev.annette.bpm_repository.api.domain.{DataSchemaId, Datatype, VariableName}
import biz.lobachev.annette.bpm_repository.api.schema.DataSchemaVariable
import io.scalaland.chimney.dsl._

case class DataSchemaVariableRecord(
  dataSchemaId: DataSchemaId,
  variableName: VariableName,
  name: String,
  caption: String,
  datatype: Datatype.Datatype,
  defaultValue: String
)

object DataSchemaVariableRecord {
  def fromDataSchemaVariables(
    dataSchemaId: DataSchemaId,
    vars: Map[String, DataSchemaVariable]
  ): Seq[DataSchemaVariableRecord] =
    vars.map {
      case k -> v =>
        v.into[DataSchemaVariableRecord]
          .withFieldConst(_.dataSchemaId, dataSchemaId)
          .withFieldConst(_.variableName, VariableName(k))
          .transform
    }.toSeq

  def toDataSchemaVariableMap(
    recs: Seq[DataSchemaVariableRecord]
  ): Map[String, DataSchemaVariable] =
    recs.map(r => r.variableName.value -> r.transformInto[DataSchemaVariable]).toMap

}
