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

import biz.lobachev.annette.application.api.translation.FindTranslationQuery
import biz.lobachev.annette.application.impl.translation.TranslationEntity
import biz.lobachev.annette.core.model.elastic.FindResult
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest

import scala.concurrent.Future

trait TranslationIndexDao {

  def createIndexRequest: CreateIndexRequest

  def createTranslation(event: TranslationEntity.TranslationCreated): Future[Unit]

  def updateTranslation(event: TranslationEntity.TranslationNameUpdated): Future[Unit]

  def deleteTranslation(event: TranslationEntity.TranslationDeleted): Future[Unit]

  def findTranslations(query: FindTranslationQuery): Future[FindResult]

}
