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
import biz.lobachev.annette.core.attribute.{AttributeMetadata, AttributeValues}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.core.model.indexing.FindResult
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

  override def getPersonById(
    id: PersonId,
    fromReadSide: Boolean = true,
    withAttributes: Option[String] = None
  ): ServiceCall[NotUsed, Person] =
    ServiceCall { _ =>
      personEntityService.getPersonById(id, fromReadSide, withAttributes)
    }

  override def getPersonsById(
    fromReadSide: Boolean,
    withAttributes: Option[String] = None
  ): ServiceCall[Set[PersonId], Seq[Person]] =
    ServiceCall { ids =>
      personEntityService.getPersonsById(ids, fromReadSide, withAttributes)
    }

  override def findPersons: ServiceCall[PersonFindQuery, FindResult]                   =
    ServiceCall { query =>
      personEntityService.findPersons(query)
    }
  override def getPersonMetadata: ServiceCall[NotUsed, Map[String, AttributeMetadata]] =
    ServiceCall { _ =>
      personEntityService.getPersonMetadata
    }

  override def updatePersonAttributes: ServiceCall[UpdatePersonAttributesPayload, Done] =
    ServiceCall { payload =>
      personEntityService.updatePersonAttributes(payload)
    }

  override def getPersonAttributes(
    id: PersonId,
    fromReadSide: Boolean,
    attributes: Option[String]
  ): ServiceCall[NotUsed, AttributeValues] =
    ServiceCall { _ =>
      personEntityService.getPersonAttributes(id, fromReadSide, attributes)
    }

  override def getPersonsAttributes(
    fromReadSide: Boolean,
    attributes: Option[String]
  ): ServiceCall[Set[PersonId], Map[String, AttributeValues]] =
    ServiceCall { ids =>
      personEntityService.getPersonsAttributes(ids, fromReadSide, attributes)
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
