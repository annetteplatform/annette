package biz.lobachev.annette.person.test

import java.time.OffsetDateTime
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, PersonPrincipal}
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.core.test.generator.RandomGenerator
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.PersonEntity.PersonCreated
import io.scalaland.chimney.dsl._

import scala.util.Random

trait PersonTestData extends RandomGenerator {

  def generateCreatePersonPayload(
    id: String = generateId,
    firstname: String = generateWord(),
    lastname: String = generateWord(),
    middlename: Option[String] = Some(generateWord()),
    categoryId: CategoryId = "PERSON",
    phone: String = s"+7${Random.nextInt(10)}",
    email: Option[String] = None,
    createdBy: AnnettePrincipal = PersonPrincipal(generateWord())
  ) =
    CreatePersonPayload(
      id = id,
      lastname = lastname,
      firstname = firstname,
      middlename = middlename,
      categoryId = categoryId,
      phone = Some(phone),
      email =
        Some(email.getOrElse(s"$firstname.$lastname@${generateWord().toLowerCase}.@${generateWord(2).toLowerCase}")),
      createdBy = createdBy
    )

  def generateUpdatePersonPayload(
    id: String = generateId,
    firstname: String = generateWord(),
    lastname: String = generateWord(),
    middlename: Option[String] = Some(generateWord()),
    categoryId: CategoryId = "PERSON",
    phone: String = s"+7${Random.nextInt(10)}",
    email: Option[String] = None,
    updatedBy: AnnettePrincipal = PersonPrincipal(generateWord())
  ) =
    UpdatePersonPayload(
      id = id,
      lastname = lastname,
      firstname = firstname,
      middlename = middlename,
      categoryId = categoryId,
      phone = Some(phone),
      email =
        Some(email.getOrElse(s"$firstname.$lastname@${generateWord().toLowerCase}.@${generateWord(2).toLowerCase}")),
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
}
