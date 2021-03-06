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
import io.scalaland.chimney.dsl._
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.persons.api.category.{
  CreateCategoryPayload,
  DeleteCategoryPayload,
  PersonCategory,
  PersonCategoryAlreadyExist,
  PersonCategoryFindQuery,
  PersonCategoryId,
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

  override def getPersonById(id: PersonId, fromReadSide: Boolean): Future[Person] =
    api.getPersonById(id, fromReadSide).invoke()

  override def getPersonsById(ids: Set[PersonId], fromReadSide: Boolean): Future[Map[PersonId, Person]] =
    api.getPersonsById(fromReadSide).invoke(ids)

  override def findPersons(query: PersonFindQuery): Future[FindResult] = api.findPersons.invoke(query)

  // org category methods

  def createCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createCategory.invoke(payload)

  def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateCategory.invoke(payload)

  def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done] =
    createCategory(payload).recoverWith {
      case PersonCategoryAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateCategoryPayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updateCategory(updatePayload)
      case th                            => Future.failed(th)
    }

  def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteCategory.invoke(payload)

  def getCategoryById(id: PersonCategoryId, fromReadSide: Boolean): Future[PersonCategory] =
    api.getCategoryById(id, fromReadSide).invoke()

  def getCategoriesById(
    ids: Set[PersonCategoryId],
    fromReadSide: Boolean
  ): Future[Map[PersonCategoryId, PersonCategory]] =
    api.getCategoriesById(fromReadSide).invoke(ids)

  def findCategories(query: PersonCategoryFindQuery): Future[FindResult] =
    api.findCategories.invoke(query)

}
