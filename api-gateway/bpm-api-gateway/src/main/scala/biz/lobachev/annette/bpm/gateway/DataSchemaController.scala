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
import biz.lobachev.annette.bpm.gateway.schema._
import biz.lobachev.annette.bpm_repository.api.BpmRepositoryService
import biz.lobachev.annette.bpm_repository.api.domain.DataSchemaId
import biz.lobachev.annette.bpm_repository.api.schema._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DataSchemaController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  bpmRepositoryService: BpmRepositoryService,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  def createDataSchema =
    authenticated.async(parse.json[CreateDataSchemaPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[CreateDataSchemaPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          dataSchema <- bpmRepositoryService.createDataSchema(payload)
        } yield Ok(Json.toJson(dataSchema))
      }
    }

  def updateDataSchema =
    authenticated.async(parse.json[UpdateDataSchemaPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateDataSchemaPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          dataSchema <- bpmRepositoryService.updateDataSchema(payload)
        } yield Ok(Json.toJson(dataSchema))
      }
    }

  def updateDataSchemaName =
    authenticated.async(parse.json[UpdateDataSchemaNamePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateDataSchemaNamePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          dataSchema <- bpmRepositoryService.updateDataSchemaName(payload)
        } yield Ok(Json.toJson(dataSchema))
      }
    }

  def updateDataSchemaDescription =
    authenticated.async(parse.json[UpdateDataSchemaDescriptionPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateDataSchemaDescriptionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          dataSchema <- bpmRepositoryService.updateDataSchemaDescription(payload)
        } yield Ok(Json.toJson(dataSchema))
      }
    }

  def storeDataSchemaVariable =
    authenticated.async(parse.json[StoreDataSchemaVariablePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[StoreDataSchemaVariablePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          dataSchema <- bpmRepositoryService.storeDataSchemaVariable(payload)
        } yield Ok(Json.toJson(dataSchema))
      }
    }

  def deleteDataSchemaVariable =
    authenticated.async(parse.json[DeleteDataSchemaVariablePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[DeleteDataSchemaVariablePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          dataSchema <- bpmRepositoryService.deleteDataSchemaVariable(payload)
        } yield Ok(Json.toJson(dataSchema))
      }
    }

  def deleteDataSchema =
    authenticated.async(parse.json[DeleteDataSchemaPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[DeleteDataSchemaPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- bpmRepositoryService.deleteDataSchema(payload)
        } yield Ok("")
      }
    }

  def getDataSchema(id: String, withVariables: Option[Boolean] = None) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          dataSchema <- bpmRepositoryService.getDataSchema(id, withVariables)
        } yield Ok(Json.toJson(dataSchema))
      }
    }

  def getDataSchemas(withVariables: Option[Boolean] = None) =
    authenticated.async(parse.json[Set[DataSchemaId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          dataSchemas <- bpmRepositoryService.getDataSchemas(request.body.toSeq, withVariables)
        } yield Ok(Json.toJson(dataSchemas))
      }
    }

  def findDataSchemas =
    authenticated.async(parse.json[DataSchemaFindQuery]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          result <- bpmRepositoryService.findDataSchemas(request.body)
        } yield Ok(Json.toJson(result))
      }
    }

}
