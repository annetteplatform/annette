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
import akka.Done
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
import biz.lobachev.annette.core.model.indexing.FindResult

import scala.concurrent.Future

class BpmRepositoryServiceImpl(api: BpmRepositoryServiceApi) extends BpmRepositoryService {
  override def createBpmModel(payload: CreateBpmModelPayload): Future[BpmModel] =
    api.createBpmModel.invoke(payload)

  def updateBpmModel(payload: UpdateBpmModelPayload): Future[BpmModel] =
    api.updateBpmModel.invoke(payload)

  override def updateBpmModelName(payload: UpdateBpmModelNamePayload): Future[BpmModel] =
    api.updateBpmModelName.invoke(payload)

  override def updateBpmModelDescription(payload: UpdateBpmModelDescriptionPayload): Future[BpmModel] =
    api.updateBpmModelDescription.invoke(payload)

  override def updateBpmModelXml(payload: UpdateBpmModelXmlPayload): Future[BpmModel] =
    api.updateBpmModelXml.invoke(payload)

  override def deleteBpmModel(id: String): Future[Done] =
    api.deleteBpmModel(id).invoke()

  override def getBpmModelById(id: String, withXml: Boolean): Future[BpmModel] =
    api.getBpmModelById(id, withXml).invoke()

  override def getBpmModelsById(ids: Seq[BpmModelId], withXml: Boolean): Future[Seq[BpmModel]] =
    api.getBpmModelsById(withXml).invoke(ids)

  override def findBpmModels(query: BpmModelFindQuery): Future[FindResult] =
    api.findBpmModels.invoke(query)
}
