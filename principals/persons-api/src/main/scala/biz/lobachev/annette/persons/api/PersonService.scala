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

package biz.lobachev.annette.persons.api
import akka.Done
import biz.lobachev.annette.core.attribute.{AttributeMetadata, AttributeValues, UpdateAttributesPayload}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.persons.api.person._

import scala.concurrent.Future

trait PersonService {

  def createPerson(payload: CreatePersonPayload): Future[Done]
  def updatePerson(payload: UpdatePersonPayload): Future[Done]
  def createOrUpdatePerson(payload: CreatePersonPayload): Future[Done]
  def deletePerson(payload: DeletePersonPayload): Future[Done]
  def getPerson(id: PersonId, source: Option[String] = None, attributes: Option[String] = None): Future[Person]
  def getPersons(
    ids: Set[PersonId],
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[Seq[Person]]
  def findPersons(query: PersonFindQuery): Future[FindResult]
  def getPersonMetadata: Future[Map[String, AttributeMetadata]]
  def updatePersonAttributes(payload: UpdateAttributesPayload): Future[Done]
  def getPersonAttributes(
    id: PersonId,
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[AttributeValues]
  def getPersonsAttributes(
    ids: Set[PersonId],
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[Map[String, AttributeValues]]

  // category methods

  def createCategory(payload: CreateCategoryPayload): Future[Done]
  def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done]
  def updateCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteCategory(payload: DeleteCategoryPayload): Future[Done]
  def getCategory(id: CategoryId, source: Option[String] = None): Future[Category]
  def getCategories(ids: Set[CategoryId], source: Option[String] = None): Future[Seq[Category]]
  def findCategories(query: CategoryFindQuery): Future[FindResult]
}
