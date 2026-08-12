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

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.core.attribute.UpdateAttributesPayload
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryFindQuery,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.core.model.indexing.SortBy
import biz.lobachev.annette.persons.api.{grpc => g}
import biz.lobachev.annette.persons.api.grpc.PersonService
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.category.CategoryEntityService
import biz.lobachev.annette.persons.impl.person.PersonEntityService
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class PersonServiceApiImpl(
  personEntityService: PersonEntityService,
  categoryEntityService: CategoryEntityService
)(implicit ec: ExecutionContext) extends PersonService {

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal =
    AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: Person): g.Person =
    g.Person(
      id = p.id,
      lastname = p.lastname,
      firstname = p.firstname,
      middlename = p.middlename,
      categoryId = p.categoryId,
      phone = p.phone,
      email = p.email,
      source = p.source,
      externalId = p.externalId,
      attributes = p.attributes,
      updatedAt = p.updatedAt.toString,
      updatedBy = Some(fromDomain(p.updatedBy))
    )

  private def fromDomain(c: Category): g.Category =
    g.Category(
      id = c.id,
      name = c.name,
      updatedAt = c.updatedAt.toString,
      updatedBy = Some(fromDomain(c.updatedBy))
    )

  private def formatOdt(odt: OffsetDateTime): String = odt.toString

  // === Person CRUD ===

  override def createPerson(in: g.CreatePersonPayload): Future[Empty] =
    personEntityService
      .createPerson(
        CreatePersonPayload(
          id = in.id,
          lastname = in.lastname,
          firstname = in.firstname,
          middlename = in.middlename,
          categoryId = in.categoryId,
          phone = in.phone,
          email = in.email,
          source = in.source,
          externalId = in.externalId,
          attributes = if (in.attributes.isEmpty) None else Some(in.attributes),
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updatePerson(in: g.UpdatePersonPayload): Future[Empty] =
    personEntityService
      .updatePerson(
        UpdatePersonPayload(
          id = in.id,
          lastname = in.lastname,
          firstname = in.firstname,
          middlename = in.middlename,
          categoryId = in.categoryId,
          phone = in.phone,
          email = in.email,
          source = in.source,
          externalId = in.externalId,
          attributes = if (in.attributes.isEmpty) None else Some(in.attributes),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deletePerson(in: g.DeletePersonPayload): Future[Empty] =
    personEntityService
      .deletePerson(DeletePersonPayload(id = in.id, updatedBy = unwrapPrincipal(in.updatedBy)))
      .map(_ => Empty())

  override def getPerson(in: g.GetPersonRequest): Future[g.Person] =
    personEntityService.getPerson(in.id, in.source, in.attributes).map(fromDomain)

  override def getPersons(in: g.GetPersonsRequest): Future[g.GetPersonsResponse] =
    personEntityService
      .getPersons(in.ids.toSet, in.source, in.attributes)
      .map(persons => g.GetPersonsResponse(persons = persons.map(fromDomain)))

  override def findPersons(in: g.PersonFindQuery): Future[g.FindResult] =
    personEntityService
      .findPersons(
        PersonFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          lastname = in.lastname,
          firstname = in.firstname,
          middlename = in.middlename,
          phone = in.phone,
          email = in.email,
          categories = if (in.categories.isEmpty) None else Some(in.categories.toSet),
          sources = if (in.sources.isEmpty) None else Some(in.sources.toSet),
          externalIds = if (in.externalIds.isEmpty) None else Some(in.externalIds.toSet),
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  override def getPersonMetadata(in: Empty): Future[g.GetPersonMetadataResponse] =
    personEntityService.getEntityMetadata
      .map { metadata =>
        g.GetPersonMetadataResponse(
          metadataJson = metadata.map { case (k, v) => k -> Json.stringify(Json.toJson(v)) }
        )
      }

  override def updatePersonAttributes(in: g.UpdateAttributesPayload): Future[Empty] =
    personEntityService
      .updatePersonAttributes(
        UpdateAttributesPayload(
          id = in.id,
          attributes = in.attributes,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def getPersonAttributes(in: g.GetPersonAttributesRequest): Future[g.AttributeValuesResponse] =
    personEntityService
      .getPersonAttributes(in.id, in.source, in.attributes)
      .map(values => g.AttributeValuesResponse(values = values))

  override def getPersonsAttributes(in: g.GetPersonsAttributesRequest): Future[g.PersonsAttributesResponse] =
    personEntityService
      .getPersonsAttributes(in.ids.toSet, in.source, in.attributes)
      .map { valuesMap =>
        g.PersonsAttributesResponse(
          values = valuesMap.map { case (k, v) => k -> g.AttributeValuesResponse(values = v) }
        )
      }

  // === Category CRUD ===

  override def createCategory(in: g.CreateCategoryPayload): Future[Empty] =
    categoryEntityService
      .createCategory(
        CreateCategoryPayload(id = in.id, name = in.name, createdBy = unwrapPrincipal(in.createdBy))
      )
      .map(_ => Empty())

  override def updateCategory(in: g.UpdateCategoryPayload): Future[Empty] =
    categoryEntityService
      .updateCategory(
        UpdateCategoryPayload(id = in.id, name = in.name, updatedBy = unwrapPrincipal(in.updatedBy))
      )
      .map(_ => Empty())

  override def deleteCategory(in: g.DeleteCategoryPayload): Future[Empty] =
    categoryEntityService
      .deleteCategory(DeleteCategoryPayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy)))
      .map(_ => Empty())

  override def getCategory(in: g.GetCategoryRequest): Future[g.Category] =
    categoryEntityService.getCategory(in.id, in.source).map(fromDomain)

  override def getCategories(in: g.GetCategoriesRequest): Future[g.GetCategoriesResponse] =
    categoryEntityService
      .getCategories(in.ids.toSet, in.source)
      .map(categories => g.GetCategoriesResponse(categories = categories.map(fromDomain)))

  override def findCategories(in: g.CategoryFindQuery): Future[g.FindResult] =
    categoryEntityService
      .findCategories(
        CategoryFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  // === domain → proto (response converters) ===

  private def fromDomain(f: biz.lobachev.annette.core.model.indexing.FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = formatOdt(h.updatedAt)))
    )
}
