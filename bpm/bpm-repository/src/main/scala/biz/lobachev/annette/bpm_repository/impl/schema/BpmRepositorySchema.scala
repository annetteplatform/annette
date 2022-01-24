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
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import java.time.Instant

object BpmRepositorySchema extends BpmRepositorySchemaImplicits {

  /** Таблица BPM моделей */
  class BpmModelTable(tag: Tag) extends Table[BpmModelRecord](tag, "bpm_models") {
    def id          = column[BpmModelId]("id", O.Length(BpmModelId.maxLength))
    def code        = column[Code]("code", O.Length(Code.maxLength))
    def name        = column[String]("name", O.SqlType("VARCHAR"), O.Length(128))
    def description = column[String]("description", O.SqlType("TEXT"))
    def notation    = column[Notation.Notation]("notation", O.SqlType("VARCHAR"), O.Length(Notation.maxLength))
    def xml         = column[String]("xml", O.SqlType("TEXT"))
    def updatedAt   = column[Instant]("updated_at", O.SqlType("TIMESTAMP"))
    def updatedBy   = column[AnnettePrincipal]("updated_by", O.SqlType("VARCHAR"), O.Length(100))

    def *                  =
      (id, code, name, description, notation, xml, updatedAt, updatedBy).<>(
        (BpmModelRecord.apply _).tupled,
        BpmModelRecord.unapply
      )
    def bpmModelPrimaryKey = primaryKey("bpm_model_primary_key", id)
  }
  lazy val bpmModels: TableQuery[BpmModelTable] = TableQuery[BpmModelTable]

  /** Схема данных */
  val dataDefinition: PostgresProfile.SchemaDescription = bpmModels.schema

}
