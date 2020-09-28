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

package biz.lobachev.annette.application.impl.language.dao

import akka.Done
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.impl.language.LanguageEntity
import com.datastax.driver.core.BoundStatement

import scala.collection.immutable.{Seq, _}
import scala.concurrent.Future

trait LanguageDbDao {

  def createTables(): Future[Unit]

  def prepareStatements(): Future[Done]

  def createLanguage(event: LanguageEntity.LanguageCreated): Seq[BoundStatement]

  def updateLanguage(event: LanguageEntity.LanguageUpdated): Seq[BoundStatement]

  def deleteLanguage(event: LanguageEntity.LanguageDeleted): Seq[BoundStatement]

  def getLanguageById(id: LanguageId): Future[Option[Language]]

  def getLanguages: Future[Map[LanguageId, Language]]

}
