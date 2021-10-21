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
import biz.lobachev.annette.api_gateway_core.authorization.{AuthorizationFailedException, Authorizer}
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
      val payload    = request.body
        .into[CreatePersonPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      val attributes = payload.attributes.map(_.keys.toSeq).getOrElse(Seq.empty)
      authorizer.performCheck(canMaintainPerson(payload.id, attributes)) {
        for {
          _             <- personService.createPerson(payload)
          withAttributes = if (attributes.nonEmpty) Some(attributes.mkString(","))
                           else None
          person        <- personService.getPersonById(payload.id, false, withAttributes)
        } yield Ok(Json.toJson(person))
      }
    }

  def updatePerson =
    authenticated.async(parse.json[PersonPayloadDto]) { implicit request =>
      val payload    = request.body
        .into[UpdatePersonPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      val attributes = payload.attributes.map(_.keys.toSeq).getOrElse(Seq.empty)
      authorizer.performCheck(canMaintainPerson(payload.id, attributes)) {
        for {
          _             <- personService.updatePerson(payload)
          withAttributes = if (attributes.nonEmpty) Some(attributes.mkString(","))
                           else None
          person        <- personService.getPersonById(payload.id, false, withAttributes)
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
      authorizer.performCheck(canMaintainAttributes(attributes)) {
        for {
          _             <- personService.updatePersonAttributes(payload)
          withAttributes = if (attributes.nonEmpty) Some(attributes.mkString(","))
                           else None
          person        <- personService.getPersonById(payload.id, false, withAttributes)
        } yield Ok(Json.toJson(person))
      }
    }

  def deletePerson =
    authenticated.async(parse.json[DeletePersonPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeletePersonPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainPerson(payload.id, Seq.empty)) {
        for {
          _ <- personService.deletePerson(payload)
        } yield Ok("")
      }
    }

  def getPersonById(id: PersonId, fromReadSide: Boolean, withAttributes: Option[String] = None) =
    authenticated.async { implicit request =>
      val attributes = withAttributes.map(_.split(",").toSeq).getOrElse(Seq.empty)
      authorizer.performCheck(canViewOrMaintainPerson(id, attributes)) {
        for {
          person <- personService.getPersonById(id, fromReadSide, withAttributes)
        } yield Ok(Json.toJson(person))
      }
    }

  def getPersonsById(fromReadSide: Boolean, withAttributes: Option[String] = None) =
    authenticated.async(parse.json[Set[PersonId]]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PERSON, MAINTAIN_ALL_PERSON) {
        val attributes = withAttributes.map(_.split(",").toSeq).getOrElse(Seq.empty)
        authorizer.performCheck(canViewOrMaintainAttributes(attributes)) {
          for {
            persons <- personService.getPersonsById(request.body, fromReadSide, withAttributes)
          } yield Ok(Json.toJson(persons))
        }
      }
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
        person <- personService.getPersonById(id, true)
      } yield Ok(Json.toJson(person))
    }

  def getPersonMetadata =
    authenticated.async { implicit request =>
      for {
        principalPermissions <-
          authorizer.findPermissions(VIEW_ALL_PERSON_ATTRIBUTE_META.id, VIEW_PERSON_ATTRIBUTE_META_ID)
        _                     = println("principalPermissions")
        _                     = println(principalPermissions)
        permissions           = principalPermissions.map(_.permission)
        allowAll              = permissions.contains(VIEW_ALL_PERSON_ATTRIBUTE_META)
        allowedAttributes     = permissions.filter(_.id == VIEW_PERSON_ATTRIBUTE_META_ID).map(_.arg1)
        meta                 <- if (allowAll || allowedAttributes.nonEmpty) personService.getPersonMetadata
                                else Future.failed(AuthorizationFailedException())
        filteredMeta          = if (allowAll) meta
                                else meta.filter { case k -> _ => allowedAttributes.contains(k) }
      } yield Ok(Json.toJson(filteredMeta))

    }

  def getPersonAttributes(id: PersonId, fromReadSide: Boolean, attributes: Option[String] = None) =
    authenticated.async { implicit request =>
      val attributeSeq = attributes.map(_.split(",").toSeq).getOrElse(Seq.empty)
      authorizer.performCheck(canViewOrMaintainPerson(id, attributeSeq)) {
        for {
          person <- personService.getPersonById(id, fromReadSide)
        } yield Ok(Json.toJson(person))
      }
    }

  def getPersonsAttributes(fromReadSide: Boolean, attributes: Option[String] = None) =
    authenticated.async(parse.json[Set[PersonId]]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PERSON, MAINTAIN_ALL_PERSON) {
        val attributeSeq = attributes.map(_.split(",").toSeq).getOrElse(Seq.empty)
        authorizer.performCheck(canViewOrMaintainAttributes(attributeSeq)) {
          for {
            persons <- personService.getPersonsById(request.body, fromReadSide)
          } yield Ok(Json.toJson(persons))
        }
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
          role <- personService.getCategoryById(payload.id, false)
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
          role <- personService.getCategoryById(payload.id, false)
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

  def getCategoryById(id: CategoryId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      val rules =
        if (fromReadSide) Seq(VIEW_ALL_PERSON_CATEGORIES, MAINTAIN_ALL_PERSON_CATEGORIES)
        else Seq(MAINTAIN_ALL_PERSON_CATEGORIES)
      authorizer.performCheckAny(rules: _*) {
        for {
          role <- personService.getCategoryById(id, fromReadSide)
        } yield Ok(Json.toJson(role))
      }
    }

  def getCategoriesById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      val ids   = request.body
      val rules =
        if (fromReadSide) Seq(VIEW_ALL_PERSON_CATEGORIES, MAINTAIN_ALL_PERSON_CATEGORIES)
        else Seq(MAINTAIN_ALL_PERSON_CATEGORIES)
      authorizer.performCheckAny(rules: _*) {
        for {
          roles <- personService.getCategoriesById(ids, fromReadSide)
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

  private def canMaintainAttributes[A](attributes: Seq[String])(implicit
    request: AuthenticatedRequest[A]
  ): Future[Boolean] =
    for {
      principalPermissions <- authorizer.findPermissions(CHANGE_ALL_PERSON_ATTRIBUTES.id, CHANGE_PERSON_ATTRIBUTE_ID)
      permissions           = principalPermissions.map(_.permission)
      allowAll              = permissions.contains(CHANGE_ALL_PERSON_ATTRIBUTES)
      allowSpecified        = if (!allowAll) {
                                val allowedAttributes = permissions.filter(_.id == CHANGE_PERSON_ATTRIBUTE_ID).map(_.arg1)
                                val attributeSet      = attributes.toSet
                                allowedAttributes.intersect(attributeSet).size == attributeSet.size
                              } else true
    } yield allowAll || allowSpecified

  private def canMaintainPerson[A](personId: PersonId, attributes: Seq[String])(implicit
    request: AuthenticatedRequest[A]
  ): Future[Boolean] =
    for {
      allowAll         <- authorizer.checkAny(MAINTAIN_ALL_PERSON)
      result           <- if (!allowAll) canMaintainPersonForOrgUnits(personId)
                          else Future.successful(true)
      attributesResult <- if (attributes.nonEmpty) canMaintainAttributes(attributes)
                          else Future.successful(true)
    } yield result && attributesResult

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

  private def canViewOrMaintainAttributes[A](attributes: Seq[String])(implicit
    request: AuthenticatedRequest[A]
  ): Future[Boolean] =
    for {
      principalPermissions <- authorizer.findPermissions(
                                CHANGE_ALL_PERSON_ATTRIBUTES.id,
                                CHANGE_PERSON_ATTRIBUTE_ID,
                                VIEW_ALL_PERSON_ATTRIBUTES.id,
                                VIEW_PERSON_ATTRIBUTE_ID
                              )
      permissions           = principalPermissions.map(_.permission)
      allowAll              = permissions.contains(CHANGE_ALL_PERSON_ATTRIBUTES) || permissions.contains(VIEW_ALL_PERSON_ATTRIBUTES)
      allowSpecified        = if (!allowAll) {
                                val allowedAttributes = permissions
                                  .filter(p => p.id == CHANGE_PERSON_ATTRIBUTE_ID || p.id == VIEW_PERSON_ATTRIBUTE_ID)
                                  .map(_.arg1)
                                val attributeSet      = attributes.toSet
                                allowedAttributes.intersect(attributeSet).size == attributeSet.size
                              } else true
    } yield allowAll || allowSpecified

  private def canViewOrMaintainPerson[A](
    personId: PersonId,
    attributes: Seq[String]
  )(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    for {
      allowAll         <- authorizer.checkAny(VIEW_ALL_PERSON, MAINTAIN_ALL_PERSON)
      result           <- if (!allowAll) canMaintainPersonForOrgUnits(personId)
                          else Future.successful(true)
      attributesResult <- if (attributes.nonEmpty) canViewOrMaintainAttributes(attributes)
                          else Future.successful(true)
    } yield result && attributesResult

}
