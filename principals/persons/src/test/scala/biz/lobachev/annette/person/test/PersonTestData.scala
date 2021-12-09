package biz.lobachev.annette.person.test

import java.time.OffsetDateTime
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, PersonPrincipal}
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.core.test.generator.RandomGenerator
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.PersonEntity.{PersonCreated, PersonUpdated}
import io.scalaland.chimney.dsl._

import scala.util.Random

trait PersonTestData extends RandomGenerator {

  def generateCreatePersonPayload(
    id: String = generateId,
    firstname: String = generateWord(),
    lastname: String = generateWord(),
    middlename: Option[String] = if (Random.nextBoolean()) Some(generateWord()) else None,
    categoryId: CategoryId = s"CAT-${Random.nextInt(10)}",
    phone: Option[String] = if (Random.nextBoolean()) Some(s"+7${Random.nextInt(100000)}") else None,
    email: Option[String] = if (Random.nextBoolean()) Some(s"user-${Random.nextInt(1000)}@example.com") else None,
    source: Option[String] = if (Random.nextBoolean()) Some(s"SOURCE-${Random.nextInt(10)}") else None,
    externalId: Option[String] = if (Random.nextBoolean()) Some(s"ID-${Random.nextInt(10000)}") else None,
    createdBy: AnnettePrincipal = PersonPrincipal(generateWord())
  ) =
    CreatePersonPayload(
      id = id,
      lastname = lastname,
      firstname = firstname,
      middlename = middlename,
      categoryId = categoryId,
      phone = phone,
      email = email,
      source = source,
      externalId = externalId,
      createdBy = createdBy
    )

  def generateUpdatePersonPayload(
    id: String = generateId,
    firstname: String = generateWord(),
    lastname: String = generateWord(),
    middlename: Option[String] = if (Random.nextBoolean()) Some(generateWord()) else None,
    categoryId: CategoryId = s"CAT-${Random.nextInt(10)}",
    phone: Option[String] = if (Random.nextBoolean()) Some(s"+7${Random.nextInt(100000)}") else None,
    email: Option[String] = if (Random.nextBoolean()) Some(s"user-${Random.nextInt(1000)}@example.com") else None,
    source: Option[String] = if (Random.nextBoolean()) Some(s"SOURCE-${Random.nextInt(10)}") else None,
    externalId: Option[String] = if (Random.nextBoolean()) Some(s"ID-${Random.nextInt(10000)}") else None,
    updatedBy: AnnettePrincipal = PersonPrincipal(generateWord())
  ) =
    UpdatePersonPayload(
      id = id,
      lastname = lastname,
      firstname = firstname,
      middlename = middlename,
      categoryId = categoryId,
      phone = phone,
      email = email,
      source = source,
      externalId = externalId,
      updatedBy = updatedBy
    )

  def generateDeletePersonPayload(
    id: String = generateId,
    updatedBy: AnnettePrincipal = PersonPrincipal(generateWord())
  ) =
    DeletePersonPayload(
      id = id,
      updatedBy
    )

  def convertToPerson(payload: CreatePersonPayload, createdAt: OffsetDateTime) =
    payload
      .into[Person]
      .withFieldComputed(_.updatedBy, _.createdBy)
      .withFieldComputed(_.attributes, _.attributes.getOrElse(Map.empty))
      .withFieldConst(_.updatedAt, createdAt)
      .transform

  def convertToPerson(payload: UpdatePersonPayload, updatedAt: OffsetDateTime) =
    payload
      .into[Person]
      .withFieldComputed(_.updatedBy, _.updatedBy)
      .withFieldComputed(_.attributes, _.attributes.getOrElse(Map.empty))
      .withFieldConst(_.updatedAt, updatedAt)
      .transform

  def convertToPersonCreated(payload: CreatePersonPayload, createdAt: OffsetDateTime = OffsetDateTime.now) =
    payload
      .into[PersonCreated]
      .withFieldComputed(_.createdBy, _.createdBy)
      .withFieldConst(_.createdAt, createdAt)
      .transform

  def convertToPersonUpdated(payload: UpdatePersonPayload, updateAt: OffsetDateTime = OffsetDateTime.now) =
    payload
      .into[PersonUpdated]
      .withFieldComputed(_.updatedBy, _.updatedBy)
      .withFieldConst(_.updatedAt, updateAt)
      .transform
}
