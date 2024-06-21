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
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryAlreadyExist,
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.persons.api.person._

import scala.concurrent.{ExecutionContext, Future}

class PersonServiceImpl(api: PersonServiceApi, implicit val ec: ExecutionContext) extends PersonService {
  override def createPerson(payload: CreatePersonPayload): Future[Done] = api.createPerson.invoke(payload)

  override def updatePerson(payload: UpdatePersonPayload): Future[Done] = api.updatePerson.invoke(payload)

  def createOrUpdatePerson(payload: CreatePersonPayload): Future[Done] =
    createPerson(payload).recoverWith {
      case PersonAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdatePersonPayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updatePerson(updatePayload)
      case th                    => Future.failed(th)
    }

  override def deletePerson(payload: DeletePersonPayload): Future[Done] = api.deletePerson.invoke(payload)

  override def getPerson(
    id: PersonId,
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[Person] =
    api.getPerson(id, source, attributes).invoke()

  override def getPersons(
    ids: Set[PersonId],
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[Seq[Person]] =
    api.getPersons(source, attributes).invoke(ids)

  override def findPersons(query: PersonFindQuery): Future[FindResult] = api.findPersons.invoke(query)

  override def getPersonMetadata: Future[Map[String, AttributeMetadata]] = api.getPersonMetadata.invoke()

  override def updatePersonAttributes(payload: UpdateAttributesPayload): Future[Done] =
    api.updatePersonAttributes.invoke(payload)

  override def getPersonAttributes(
    id: PersonId,
    source: Option[String] = None,
    attributes: Option[String]
  ): Future[AttributeValues] =
    api.getPersonAttributes(id, source, attributes).invoke()

  override def getPersonsAttributes(
    ids: Set[PersonId],
    source: Option[String] = None,
    attributes: Option[String]
  ): Future[Map[String, AttributeValues]] =
    api.getPersonsAttributes(source, attributes).invoke(ids)

  // org category methods

  def createCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createCategory.invoke(payload)

  def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateCategory.invoke(payload)

  def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done] =
    createCategory(payload).recoverWith {
      case CategoryAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateCategoryPayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updateCategory(updatePayload)
      case th                      => Future.failed(th)
    }

  def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteCategory.invoke(payload)

  def getCategory(id: CategoryId, source: Option[String] = None): Future[Category] =
    api.getCategory(id, source).invoke()

  def getCategories(ids: Set[CategoryId], source: Option[String] = None): Future[Seq[Category]] =
    api.getCategories(source).invoke(ids)

  def findCategories(query: CategoryFindQuery): Future[FindResult] =
    api.findCategories.invoke(query)

}
