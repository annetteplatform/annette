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

package biz.lobachev.annette.application.impl.translation.dao

import java.time.OffsetDateTime

import akka.Done
import biz.lobachev.annette.application.api.language.LanguageId
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation.TranslationEntity
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.datastax.driver.core.BoundStatement

import scala.collection.immutable.{Seq, _}
import scala.concurrent.Future

trait TranslationDbDao {

  def createTables(): Future[Unit]

  def prepareStatements(): Future[Done]

  def deleteTranslation(event: TranslationEntity.TranslationDeleted): Seq[BoundStatement]

  def changeTranslationJson(
    id: TranslationId,
    languageId: LanguageId,
    json: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): Seq[BoundStatement]

  def getTranslationJsonById(id: TranslationId, languageId: LanguageId): Future[Option[TranslationJson]]

  def getTranslationJsonsById(
    ids: Set[TranslationId],
    languageId: LanguageId
  ): Future[Map[TranslationId, TranslationJson]]

}
