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

import biz.lobachev.annette.bpm_repository.api.bp.BusinessProcessVariable
import biz.lobachev.annette.bpm_repository.api.domain.{BusinessProcessId, Datatype, VariableName}
import io.scalaland.chimney.dsl._

case class BusinessProcessVariableRecord(
  businessProcessId: BusinessProcessId,
  variableName: VariableName,
  name: String,
  caption: String,
  datatype: Datatype.Datatype,
  defaultValue: String
)

object BusinessProcessVariableRecord {
  def fromBusinessProcessVariables(
    businessProcessId: BusinessProcessId,
    vars: Map[String, BusinessProcessVariable]
  ): Seq[BusinessProcessVariableRecord] =
    vars.map {
      case k -> v =>
        v.into[BusinessProcessVariableRecord]
          .withFieldConst(_.businessProcessId, businessProcessId)
          .withFieldConst(_.variableName, VariableName(k))
          .transform
    }.toSeq

  def toBusinessProcessVariableMap(
    recs: Seq[BusinessProcessVariableRecord]
  ): Map[String, BusinessProcessVariable] =
    recs.map(r => r.variableName.value -> r.transformInto[BusinessProcessVariable]).toMap

}
