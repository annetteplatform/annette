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
import biz.lobachev.annette.core.attribute.{AttributeMetadata, AttributeValues, UpdateAttributesPayload}
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

  override def getPerson(
    id: PersonId,
    source: Option[String] = None,
    withAttributes: Option[String] = None
  ): ServiceCall[NotUsed, Person] =
    ServiceCall { _ =>
      personEntityService.getPerson(id, source, withAttributes)
    }

  override def getPersons(
    source: Option[String] = None,
    withAttributes: Option[String] = None
  ): ServiceCall[Set[PersonId], Seq[Person]] =
    ServiceCall { ids =>
      personEntityService.getPersons(ids, source, withAttributes)
    }

  override def findPersons: ServiceCall[PersonFindQuery, FindResult]                   =
    ServiceCall { query =>
      personEntityService.findPersons(query)
    }
  override def getPersonMetadata: ServiceCall[NotUsed, Map[String, AttributeMetadata]] =
    ServiceCall { _ =>
      personEntityService.getEntityMetadata
    }

  override def updatePersonAttributes: ServiceCall[UpdateAttributesPayload, Done] =
    ServiceCall { payload =>
      personEntityService.updatePersonAttributes(payload)
    }

  override def getPersonAttributes(
    id: PersonId,
    source: Option[String] = None,
    attributes: Option[String]
  ): ServiceCall[NotUsed, AttributeValues] =
    ServiceCall { _ =>
      personEntityService.getPersonAttributes(id, source, attributes)
    }

  override def getPersonsAttributes(
    source: Option[String] = None,
    attributes: Option[String]
  ): ServiceCall[Set[PersonId], Map[String, AttributeValues]] =
    ServiceCall { ids =>
      personEntityService.getPersonsAttributes(ids, source, attributes)
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

  override def getCategory(id: CategoryId, source: Option[String] = None): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      categoryEntityService.getCategory(id, source)
    }

  override def getCategories(
    source: Option[String] = None
  ): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      categoryEntityService.getCategories(ids, source)
    }

  override def findCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      categoryEntityService.findCategories(query)
    }

}
