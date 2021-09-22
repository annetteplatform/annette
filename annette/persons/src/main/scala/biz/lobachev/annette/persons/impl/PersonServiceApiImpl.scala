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

package biz.lobachev.annette.persons.impl

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.persons.impl.category.CategoryEntityService
import biz.lobachev.annette.persons.api.PersonServiceApi
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.PersonEntityService
import com.lightbend.lagom.scaladsl.api.ServiceCall

class PersonServiceApiImpl(
  personEntityService: PersonEntityService,
  categoryEntityService: CategoryEntityService
) extends PersonServiceApi {

  override def createPerson: ServiceCall[CreatePersonPayload, Done] =
    ServiceCall { payload =>
      personEntityService.createPerson(payload)
    }

  override def updatePerson: ServiceCall[UpdatePersonPayload, Done] =
    ServiceCall { payload =>
      personEntityService.updatePerson(payload)
    }

  override def deletePerson: ServiceCall[DeletePersonPayload, Done] =
    ServiceCall { payload =>
      personEntityService.deletePerson(payload)
    }

  override def getPersonById(id: PersonId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Person] =
    ServiceCall { _ =>
      personEntityService.getPersonById(id, fromReadSide)
    }

  override def getPersonsById(fromReadSide: Boolean): ServiceCall[Set[PersonId], Seq[Person]] =
    ServiceCall { ids =>
      personEntityService.getPersonsById(ids, fromReadSide)
    }

  override def findPersons: ServiceCall[PersonFindQuery, FindResult] =
    ServiceCall { query =>
      personEntityService.findPersons(query)
    }

  // ****************************** Category methods ******************************

  override def createCategory: ServiceCall[CreateCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.createCategory(payload)
    }

  override def updateCategory: ServiceCall[UpdateCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.updateCategory(payload)
    }

  override def deleteCategory: ServiceCall[DeleteCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.deleteCategory(payload)
    }

  override def getCategoryById(id: CategoryId, fromReadSide: Boolean): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      categoryEntityService.getCategoryById(id, fromReadSide)
    }

  override def getCategoriesById(
    fromReadSide: Boolean
  ): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      categoryEntityService.getCategoriesById(ids, fromReadSide)
    }

  override def findCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      categoryEntityService.findCategories(query)
    }

}
