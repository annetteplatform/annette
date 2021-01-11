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

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.persons.api.category.{
  CreateCategoryPayload,
  DeleteCategoryPayload,
  PersonCategory,
  PersonCategoryFindQuery,
  PersonCategoryId,
  UpdateCategoryPayload
}
import biz.lobachev.annette.persons.api.person._
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait PersonServiceApi extends Service {

  /**
   * Creates person.
   * The following business rules are applied:
   *  * If person already exists and active it returns exception 400 Person already exist.
   *
   * @return
   */
  def createPerson: ServiceCall[CreatePersonPayload, Done]

  /**
   * Updates person.
   * The following business rules are applied:
   *  * If person don't exist, it returns exception 404 Person not found.
   *
   * @return
   */
  def updatePerson: ServiceCall[UpdatePersonPayload, Done]

  /**
   * Deletes person.
   * If person don't exist, it returns exception 404 Person not found.
   *
   * @return
   */
  def deletePerson: ServiceCall[DeletePersonPayload, Done]

  /**
   * Returns person by person id. If person doesn't exist, it returns exception 404 Person not found.
   *
   * @param id
   * @return
   */
  def getPersonById(id: PersonId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Person]

  /**
   * Returns persons by person ids.
   *
   * @return
   */
  def getPersonsById(fromReadSide: Boolean = true): ServiceCall[Set[PersonId], Map[PersonId, Person]]

  /**
   * Search person using particular query.
   *
   * @return
   */
  def findPersons: ServiceCall[PersonFindQuery, FindResult]

  // org item category

  def createCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getCategoryById(id: PersonCategoryId, fromReadSide: Boolean): ServiceCall[NotUsed, PersonCategory]
  def getCategoriesById(
    fromReadSide: Boolean
  ): ServiceCall[Set[PersonCategoryId], Map[PersonCategoryId, PersonCategory]]
  def findCategories: ServiceCall[PersonCategoryFindQuery, FindResult]

  final override def descriptor = {
    import Service._
    // @formatter:off
    named("persons")
      .withCalls(
        pathCall("/api/persons/v1/createPerson",                     createPerson),
        pathCall("/api/persons/v1/updatePerson",                     updatePerson),
        pathCall("/api/persons/v1/deletePerson",                     deletePerson),
        pathCall("/api/persons/v1/getPersonById/:id/:fromReadSide",  getPersonById _),
        pathCall("/api/persons/v1/getPersonsById/:fromReadSide",     getPersonsById _),
        pathCall("/api/persons/v1/findPersons",                      findPersons),

        pathCall("/api/persons/v1/createCategory",                 createCategory),
        pathCall("/api/persons/v1/updateCategory",                 updateCategory),
        pathCall("/api/persons/v1/deleteCategory",                 deleteCategory),
        pathCall("/api/persons/v1/getCategoryById/:id/:readSide",  getCategoryById _),
        pathCall("/api/persons/v1/getCategoriesById/:readSide",    getCategoriesById _) ,
        pathCall("/api/persons/v1/findCategories",                 findCategories),
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
    // @formatter:on
  }
}
