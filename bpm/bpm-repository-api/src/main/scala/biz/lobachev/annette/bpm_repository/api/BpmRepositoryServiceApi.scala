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

package biz.lobachev.annette.bpm_repository.api

import akka.{Done, NotUsed}
import biz.lobachev.annette.bpm_repository.api.domain.BpmModelId
import biz.lobachev.annette.bpm_repository.api.model.{
  BpmModel,
  BpmModelFindQuery,
  CreateBpmModelPayload,
  UpdateBpmModelDescriptionPayload,
  UpdateBpmModelNamePayload,
  UpdateBpmModelPayload,
  UpdateBpmModelXmlPayload
}
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait BpmRepositoryServiceApi extends Service {

  def createBpmModel: ServiceCall[CreateBpmModelPayload, BpmModel]
  def updateBpmModel: ServiceCall[UpdateBpmModelPayload, BpmModel]
  def updateBpmModelName: ServiceCall[UpdateBpmModelNamePayload, BpmModel]
  def updateBpmModelDescription: ServiceCall[UpdateBpmModelDescriptionPayload, BpmModel]
  def updateBpmModelXml: ServiceCall[UpdateBpmModelXmlPayload, BpmModel]
  def deleteBpmModel(id: String): ServiceCall[NotUsed, Done]
  def getBpmModelById(id: String, withXml: Boolean): ServiceCall[NotUsed, BpmModel]
  def getBpmModelsById(withXml: Boolean): ServiceCall[Seq[BpmModelId], Seq[BpmModel]]
  def findBpmModels: ServiceCall[BpmModelFindQuery, FindResult]

  final override def descriptor = {
    import Service._
    named("bpm-repository")
      .withCalls(
        pathCall("/api/bpm-repository/v1/createBpmModel", createBpmModel),
        pathCall("/api/bpm-repository/v1/updateBpmModel", updateBpmModel),
        pathCall("/api/bpm-repository/v1/updateBpmModelName", updateBpmModelName),
        pathCall("/api/bpm-repository/v1/updateBpmModelDescription", updateBpmModelDescription),
        pathCall("/api/bpm-repository/v1/updateBpmModelXml", updateBpmModelXml),
        pathCall("/api/bpm-repository/v1/deleteBpmModel", deleteBpmModel _),
        pathCall("/api/bpm-repository/v1/getBpmModelById/:id?withXml", getBpmModelById _),
        pathCall("/api/bpm-repository/v1/getBpmModelsById?withXml", getBpmModelsById _),
        pathCall("/api/bpm-repository/v1/findBpmModels", findBpmModels)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
