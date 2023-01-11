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

package biz.lobachev.annette.person.gateway

import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticatedAction, AuthenticatedRequest}
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.core.attribute.{UpdateAttributesPayload, UpdateAttributesPayloadDto}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.{DescendantUnitPrincipal, DirectUnitPrincipal, UnitChiefPrincipal}
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.person.gateway.Permissions._
import biz.lobachev.annette.person.gateway.dto._
import biz.lobachev.annette.persons.api.PersonService
import biz.lobachev.annette.persons.api.person.{
  CreatePersonPayload,
  DeletePersonPayload,
  PersonFindQuery,
  UpdatePersonPayload
}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  personService: PersonService,
  orgStructureService: OrgStructureService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  // private val log = LoggerFactory.getLogger(this.getClass)

  def createPerson =
    authenticated.async(parse.json[PersonPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreatePersonPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainPerson(payload.id)) {
        for {
          _             <- personService.createPerson(payload)
          attributes     = payload.attributes.map(_.keys.toSeq).getOrElse(Seq.empty)
          withAttributes = if (attributes.nonEmpty) Some(attributes.mkString(","))
                           else None
          person        <- personService.getPerson(payload.id, false, withAttributes)
        } yield Ok(Json.toJson(person))
      }
    }

  def updatePerson =
    authenticated.async(parse.json[PersonPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdatePersonPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainPerson(payload.id)) {
        for {
          _             <- personService.updatePerson(payload)
          attributes     = payload.attributes.map(_.keys.toSeq).getOrElse(Seq.empty)
          withAttributes = if (attributes.nonEmpty) Some(attributes.mkString(","))
                           else None
          person        <- personService.getPerson(payload.id, false, withAttributes)
        } yield Ok(Json.toJson(person))
      }
    }

  def updatePersonAttributes =
    authenticated.async(parse.json[UpdateAttributesPayloadDto]) { implicit request =>
      val payload    = request.body
        .into[UpdateAttributesPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      val attributes = payload.attributes.keys.toSeq
      authorizer.performCheck(canMaintainPerson(payload.id)) {
        for {
          _             <- personService.updatePersonAttributes(payload)
          withAttributes = if (attributes.nonEmpty) Some(attributes.mkString(","))
                           else None
          person        <- personService.getPerson(payload.id, false, withAttributes)
        } yield Ok(Json.toJson(person))
      }
    }

  def deletePerson =
    authenticated.async(parse.json[DeletePersonPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeletePersonPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainPerson(payload.id)) {
        for {
          _ <- personService.deletePerson(payload)
        } yield Ok("")
      }
    }

  def getPerson(id: PersonId, fromReadSide: Boolean, withAttributes: Option[String] = None) =
    authenticated.async { implicit request =>
      def action =
        for {
          person <- personService.getPerson(id, fromReadSide, withAttributes)
        } yield Ok(Json.toJson(person))

      if (fromReadSide) authorizer.performCheck(canViewOrMaintainPerson(id))(action)
      else authorizer.performCheck(canMaintainPerson(id))(action)

    }

  def getPersons(fromReadSide: Boolean, withAttributes: Option[String] = None) =
    authenticated.async(parse.json[Set[PersonId]]) { implicit request =>
      def action =
        for {
          persons <- personService.getPersons(request.body, fromReadSide, withAttributes)
        } yield Ok(Json.toJson(persons))
      if (fromReadSide) authorizer.performCheckAny(VIEW_ALL_PERSON, MAINTAIN_ALL_PERSON)(action)
      else authorizer.performCheckAny(MAINTAIN_ALL_PERSON)(action)
    }

  def findPersons =
    authenticated.async(parse.json[PersonFindQuery]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PERSON, MAINTAIN_ALL_PERSON) {
        for {
          result <- personService.findPersons(request.body)
        } yield Ok(Json.toJson(result))
      }

    }

  def profile =
    authenticated.async { implicit request =>
      val id = request.subject.principals.head.principalId
      for {
        person <- personService.getPerson(id, true)
      } yield Ok(Json.toJson(person))
    }

  def getPersonMetadata =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PERSON, MAINTAIN_ALL_PERSON) {
        for {
          meta <- personService.getPersonMetadata
        } yield Ok(Json.toJson(meta))
      }
    }

  def getPersonAttributes(id: PersonId, fromReadSide: Boolean, attributes: Option[String] = None) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canViewOrMaintainPerson(id)) {
        for {
          attributes <- personService.getPersonAttributes(id, fromReadSide, attributes)
        } yield Ok(Json.toJson(attributes))
      }
    }

  def getPersonsAttributes(fromReadSide: Boolean, attributes: Option[String] = None) =
    authenticated.async(parse.json[Set[PersonId]]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PERSON, MAINTAIN_ALL_PERSON) {
        for {
          attributes <- personService.getPersonsAttributes(request.body, fromReadSide, attributes)
        } yield Ok(Json.toJson(attributes))
      }
    }

  // category methods

  def createCategory =
    authenticated.async(parse.json[PersonCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PERSON_CATEGORIES) {
        val payload = request.body
          .into[CreateCategoryPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- personService.createCategory(payload)
          role <- personService.getCategory(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateCategory =
    authenticated.async(parse.json[PersonCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PERSON_CATEGORIES) {
        val payload = request.body
          .into[UpdateCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- personService.updateCategory(payload)
          role <- personService.getCategory(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteCategory =
    authenticated.async(parse.json[DeletePersonCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PERSON_CATEGORIES) {
        val payload = request.body
          .into[DeleteCategoryPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- personService.deleteCategory(payload)
        } yield Ok("")
      }
    }

  def getCategory(id: CategoryId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      val rules =
        if (fromReadSide) Seq(VIEW_ALL_PERSON_CATEGORIES, MAINTAIN_ALL_PERSON_CATEGORIES)
        else Seq(MAINTAIN_ALL_PERSON_CATEGORIES)
      authorizer.performCheckAny(rules: _*) {
        for {
          role <- personService.getCategory(id, fromReadSide)
        } yield Ok(Json.toJson(role))
      }
    }

  def getCategories(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      val ids   = request.body
      val rules =
        if (fromReadSide) Seq(VIEW_ALL_PERSON_CATEGORIES, MAINTAIN_ALL_PERSON_CATEGORIES)
        else Seq(MAINTAIN_ALL_PERSON_CATEGORIES)
      authorizer.performCheckAny(rules: _*) {
        for {
          roles <- personService.getCategories(ids, fromReadSide)
        } yield Ok(Json.toJson(roles))
      }
    }

  def findCategories =
    authenticated.async(parse.json[CategoryFindQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(VIEW_ALL_PERSON_CATEGORIES, MAINTAIN_ALL_PERSON_CATEGORIES) {
        for {
          result <- personService.findCategories(query)
        } yield Ok(Json.toJson(result))
      }
    }

  private def canMaintainPerson[A](personId: PersonId)(implicit
    request: AuthenticatedRequest[A]
  ): Future[Boolean] =
    for {
      allowAll <- authorizer.checkAny(MAINTAIN_ALL_PERSON)
      result   <- if (!allowAll) canMaintainPersonForOrgUnits(personId)
                  else Future.successful(true)
    } yield result

  private def canMaintainPersonForOrgUnits[A](
    personId: PersonId
  )(implicit request: AuthenticatedRequest[A]): Future[Boolean] = {
    val personOrgUnitsFuture = for {
      personPrincipals <- orgStructureService.getPersonPrincipals(personId)
      personOrgUnits    = personPrincipals.map {
                            case DirectUnitPrincipal(orgUnitId)     => Some(orgUnitId)
                            case DescendantUnitPrincipal(orgUnitId) => Some(orgUnitId)
                            case _                                  => None
                          }.flatten
    } yield personOrgUnits

    for {
      maintainSubordinates       <- authorizer.checkAny(MAINTAIN_SUBORDINATE_PERSON)
      chiefOrgUnitIds             = if (maintainSubordinates)
                                      request.subject.principals
                                        .filter(_.principalType == UnitChiefPrincipal.PRINCIPAL_TYPE)
                                        .map(_.principalId)
                                        .toSet
                                    else Set.empty[String]
      maintainOrgUnitPermissions <- authorizer.findPermissions(MAINTAIN_ORG_UNIT_PERSON_PERMISSION_ID)
      maintainOrgUnitIds          = maintainOrgUnitPermissions.map(_.permission.arg1).toSet
      personOrgUnitIds           <- personOrgUnitsFuture

    } yield (chiefOrgUnitIds & maintainOrgUnitIds & personOrgUnitIds).size > 0
  }

  private def canViewOrMaintainPerson[A](
    personId: PersonId
  )(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    for {
      allowAll <- authorizer.checkAny(VIEW_ALL_PERSON, MAINTAIN_ALL_PERSON)
      result   <- if (!allowAll) canMaintainPersonForOrgUnits(personId)
                  else Future.successful(true)
    } yield result

}
