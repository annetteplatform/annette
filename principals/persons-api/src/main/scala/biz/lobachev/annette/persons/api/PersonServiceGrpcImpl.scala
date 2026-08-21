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

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.core.attribute.{AttributeMetadata, UpdateAttributesPayload}
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.core.model.category.CategoryAlreadyExist
import biz.lobachev.annette.persons.api.{grpc => g}
import org.apache.pekko.Done
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class PersonServiceGrpcImpl(client: g.PersonServiceClient)(implicit val ec: ExecutionContext) extends PersonService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

  private def sortByToProto(sortBy: Option[Seq[SortBy]]): Seq[g.SortBy] =
    sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)

  private def findResultFromProto(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def fromDomain(p: CreatePersonPayload): g.CreatePersonPayload =
    g.CreatePersonPayload(
      id = p.id,
      lastname = p.lastname,
      firstname = p.firstname,
      middlename = p.middlename,
      categoryId = p.categoryId,
      phone = p.phone,
      email = p.email,
      source = p.source,
      externalId = p.externalId,
      attributes = p.attributes.getOrElse(Map.empty),
      createdBy = Some(fromDomain(p.createdBy))
    )

  private def fromDomain(p: UpdatePersonPayload): g.UpdatePersonPayload =
    g.UpdatePersonPayload(
      id = p.id,
      lastname = p.lastname,
      firstname = p.firstname,
      middlename = p.middlename,
      categoryId = p.categoryId,
      phone = p.phone,
      email = p.email,
      source = p.source,
      externalId = p.externalId,
      attributes = p.attributes.getOrElse(Map.empty),
      updatedBy = Some(fromDomain(p.updatedBy))
    )

  private def fromDomain(p: g.Person): Person =
    Person(
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
      updatedAt = OffsetDateTime.parse(p.updatedAt),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def fromDomain(c: g.Category): Category =
    Category(
      id = c.id,
      name = c.name,
      updatedAt = OffsetDateTime.parse(c.updatedAt),
      updatedBy = unwrapPrincipal(c.updatedBy)
    )

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  override def createPerson(payload: CreatePersonPayload): Future[Done] =
    call(client.createPerson(fromDomain(payload))).map(_ => Done)

  override def updatePerson(payload: UpdatePersonPayload): Future[Done] =
    call(client.updatePerson(fromDomain(payload))).map(_ => Done)

  override def createOrUpdatePerson(payload: CreatePersonPayload): Future[Done] =
    createPerson(payload).recoverWith {
      case PersonAlreadyExist(_) =>
        updatePerson(
          UpdatePersonPayload(
            id = payload.id,
            lastname = payload.lastname,
            firstname = payload.firstname,
            middlename = payload.middlename,
            categoryId = payload.categoryId,
            phone = payload.phone,
            email = payload.email,
            source = payload.source,
            externalId = payload.externalId,
            attributes = payload.attributes,
            updatedBy = payload.createdBy
          )
        )
    }

  override def deletePerson(payload: DeletePersonPayload): Future[Done] =
    call(
      client.deletePerson(g.DeletePersonPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def getPerson(id: PersonId, source: Option[String], attributes: Option[String]): Future[Person] =
    call(client.getPerson(g.GetPersonRequest(id = id, source = source, attributes = attributes)))
      .map(fromDomain)

  override def getPersons(
    ids: Set[PersonId],
    source: Option[String],
    attributes: Option[String]
  ): Future[Seq[Person]] =
    call(
      client.getPersons(g.GetPersonsRequest(ids = ids.toSeq, source = source, attributes = attributes))
    ).map(_.persons.map(fromDomain))

  override def findPersons(query: PersonFindQuery): Future[FindResult] =
    call(
      client.findPersons(
        g.PersonFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          lastname = query.lastname,
          firstname = query.firstname,
          middlename = query.middlename,
          phone = query.phone,
          email = query.email,
          categories = query.categories.map(_.toSeq).getOrElse(Seq.empty),
          sources = query.sources.map(_.toSeq).getOrElse(Seq.empty),
          externalIds = query.externalIds.map(_.toSeq).getOrElse(Seq.empty),
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(findResultFromProto)

  override def getPersonMetadata: Future[Map[String, AttributeMetadata]] =
    call(client.getPersonMetadata(Empty()))
      .map(_.metadataJson.map { case (k, v) => k -> Json.parse(v).as[AttributeMetadata] })

  override def updatePersonAttributes(payload: UpdateAttributesPayload): Future[Done] =
    call(
      client.updatePersonAttributes(
        g.UpdateAttributesPayload(
          id = payload.id,
          attributes = payload.attributes,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def getPersonAttributes(id: PersonId, source: Option[String], attributes: Option[String]): Future[
    Map[String, String]
  ] =
    call(
      client.getPersonAttributes(g.GetPersonAttributesRequest(id = id, source = source, attributes = attributes))
    ).map(_.values)

  override def getPersonsAttributes(
    ids: Set[PersonId],
    source: Option[String],
    attributes: Option[String]
  ): Future[Map[String, Map[String, String]]] =
    call(
      client.getPersonsAttributes(
        g.GetPersonsAttributesRequest(ids = ids.toSeq, source = source, attributes = attributes)
      )
    ).map(_.values.map { case (k, v) => k -> v.values })

  // category methods

  override def createCategory(payload: CreateCategoryPayload): Future[Done] =
    call(
      client.createCategory(
        g.CreateCategoryPayload(id = payload.id, name = payload.name, createdBy = Some(fromDomain(payload.createdBy)))
      )
    ).map(_ => Done)

  override def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done] =
    createCategory(payload).recoverWith {
      case CategoryAlreadyExist(_) =>
        updateCategory(
          UpdateCategoryPayload(id = payload.id, name = payload.name, updatedBy = payload.createdBy)
        )
    }

  override def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    call(
      client.updateCategory(
        g.UpdateCategoryPayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    call(
      client.deleteCategory(
        g.DeleteCategoryPayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  override def getCategory(id: CategoryId, source: Option[String]): Future[Category] =
    call(client.getCategory(g.GetCategoryRequest(id = id, source = source))).map(fromDomain)

  override def getCategories(ids: Set[CategoryId], source: Option[String]): Future[Seq[Category]] =
    call(client.getCategories(g.GetCategoriesRequest(ids = ids.toSeq, source = source)))
      .map(_.categories.map(fromDomain))

  override def findCategories(query: CategoryFindQuery): Future[FindResult] =
    call(
      client.findCategories(
        g.CategoryFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          name = query.name,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(findResultFromProto)
}
