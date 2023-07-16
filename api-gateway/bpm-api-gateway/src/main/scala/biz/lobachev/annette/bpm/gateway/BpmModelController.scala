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

package biz.lobachev.annette.bpm.gateway

import akka.stream.Materializer
import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.bpm.gateway.model._
import biz.lobachev.annette.bpm_repository.api.BpmRepositoryService
import biz.lobachev.annette.bpm_repository.api.domain.BpmModelId
import biz.lobachev.annette.bpm_repository.api.model._
import biz.lobachev.annette.camunda.api.RepositoryService
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class BpmModelController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  bpmRepositoryService: BpmRepositoryService,
  camundaRepositoryService: RepositoryService,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  println(camundaRepositoryService)

  def createBpmModel =
    authenticated.async(parse.json[CreateBpmModelPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[CreateBpmModelPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          bpmModel <- bpmRepositoryService.createBpmModel(payload)
        } yield Ok(Json.toJson(bpmModel))
      }
    }

  def updateBpmModel =
    authenticated.async(parse.json[UpdateBpmModelPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBpmModelPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          bpmModel <- bpmRepositoryService.updateBpmModel(payload)
        } yield Ok(Json.toJson(bpmModel))
      }
    }

  def updateBpmModelName =
    authenticated.async(parse.json[UpdateBpmModelNamePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBpmModelNamePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          bpmModel <- bpmRepositoryService.updateBpmModelName(payload)
        } yield Ok(Json.toJson(bpmModel))
      }
    }

  def updateBpmModelDescription =
    authenticated.async(parse.json[UpdateBpmModelDescriptionPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBpmModelDescriptionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          bpmModel <- bpmRepositoryService.updateBpmModelDescription(payload)
        } yield Ok(Json.toJson(bpmModel))
      }
    }

  def updateBpmModelXml =
    authenticated.async(parse.json[UpdateBpmModelXmlPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBpmModelXmlPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          bpmModel <- bpmRepositoryService.updateBpmModelXml(payload)
        } yield Ok(Json.toJson(bpmModel))
      }
    }

  def deleteBpmModel =
    authenticated.async(parse.json[DeleteBpmModelPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[DeleteBpmModelPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- bpmRepositoryService.deleteBpmModel(payload)
        } yield Ok("")
      }
    }

  def getBpmModel(id: String, withXml: Option[Boolean] = None) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          bpmModel <- bpmRepositoryService.getBpmModel(id, withXml)
        } yield Ok(Json.toJson(bpmModel))
      }
    }

  def getBpmModels(withXml: Option[Boolean] = None) =
    authenticated.async(parse.json[Set[BpmModelId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          bpmModels <- bpmRepositoryService.getBpmModels(request.body.toSeq, withXml)
        } yield Ok(Json.toJson(bpmModels))
      }
    }

  def findBpmModels =
    authenticated.async(parse.json[BpmModelFindQuery]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          result <- bpmRepositoryService.findBpmModels(request.body)
        } yield Ok(Json.toJson(result))
      }
    }

}
