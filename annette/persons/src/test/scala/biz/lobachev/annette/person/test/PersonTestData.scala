package biz.lobachev.annette.person.test

import java.time.OffsetDateTime
import biz.lobachev.annette.core.model.PersonPrincipal
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, PersonPrincipal}
import biz.lobachev.annette.core.test.generator.RandomGenerator
import biz.lobachev.annette.persons.api.category.PersonCategoryId
import biz.lobachev.annette.persons.api.person._
import io.scalaland.chimney.dsl._

import scala.util.Random

trait PersonTestData extends RandomGenerator {

  def generateCreatePersonPayload(
    id: String = generateId,
    firstname: String = generateWord(),
    lastname: String = generateWord(),
    middlename: Option[String] = Some(generateWord()),
    categoryId: PersonCategoryId = "PERSON",
    phone: String = s"+7${Random.nextInt(10)}",
    email: Option[String] = None,
    createdBy: AnnettePrincipal = PersonPrincipal(generateWord())
  ) =
    CreatePersonPayload(
      id = id,
      lastname,
      firstname,
      middlename,
      categoryId,
      Some(phone),
      Some(email.getOrElse(s"$firstname.$lastname@${generateWord().toLowerCase}.@${generateWord(2).toLowerCase}")),
      createdBy
    )

  def generateUpdatePersonPayload(
    id: String = generateId,
    firstname: String = generateWord(),
    lastname: String = generateWord(),
    middlename: Option[String] = Some(generateWord()),
    categoryId: PersonCategoryId = "PERSON",
    phone: String = s"+7${Random.nextInt(10)}",
    email: Option[String] = None,
    updatedBy: AnnettePrincipal = PersonPrincipal(generateWord())
  ) =
    UpdatePersonPayload(
      id = id,
      lastname,
      firstname,
      middlename,
      categoryId,
      Some(phone),
      Some(email.getOrElse(s"$firstname.$lastname@${generateWord().toLowerCase}.@${generateWord(2).toLowerCase}")),
      updatedBy
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
      .withFieldConst(_.updatedAt, createdAt)
      .transform

  def convertToPerson(payload: UpdatePersonPayload, updatedAt: OffsetDateTime) =
    payload
      .into[Person]
      .withFieldComputed(_.updatedBy, _.updatedBy)
      .withFieldConst(_.updatedAt, updatedAt)
      .transform
}
