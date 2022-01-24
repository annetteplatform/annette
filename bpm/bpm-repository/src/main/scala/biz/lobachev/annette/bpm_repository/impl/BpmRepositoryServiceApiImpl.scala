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

package biz.lobachev.annette.bpm_repository.impl

import akka.{Done, NotUsed}
import biz.lobachev.annette.bpm_repository.api.BpmRepositoryServiceApi
import biz.lobachev.annette.bpm_repository.api.domain.BpmModelId
import biz.lobachev.annette.bpm_repository.api.model._
import biz.lobachev.annette.bpm_repository.impl.model.BpmModelService
import biz.lobachev.annette.bpm_repository.impl.schema.BpmRepositorySchemaImplicits
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall

class BpmRepositoryServiceApiImpl(bpmModelService: BpmModelService)
    extends BpmRepositoryServiceApi
    with BpmRepositorySchemaImplicits {

  override def createBpmModel: ServiceCall[CreateBpmModelPayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.createBpmModel(payload)
    }

  override def updateBpmModel: ServiceCall[UpdateBpmModelPayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.updateBpmModel(payload)
    }

  override def updateBpmModelName: ServiceCall[UpdateBpmModelNamePayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.updateBpmModelName(payload)
    }

  override def updateBpmModelDescription: ServiceCall[UpdateBpmModelDescriptionPayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.updateBpmModelDescription(payload)
    }

  override def updateBpmModelXml: ServiceCall[UpdateBpmModelXmlPayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.updateBpmModelXml(payload)
    }

  override def deleteBpmModel(id: String): ServiceCall[NotUsed, Done] =
    ServiceCall { _ =>
      bpmModelService.deleteBpmModel(id)
    }

  override def getBpmModelById(id: String, withXml: Boolean): ServiceCall[NotUsed, BpmModel] =
    ServiceCall { _ =>
      bpmModelService.getBpmModelById(id, withXml)
    }

  override def getBpmModelsById(withXml: Boolean): ServiceCall[Seq[BpmModelId], Seq[BpmModel]] =
    ServiceCall { ids =>
      bpmModelService.getBpmModelsById(ids, withXml)
    }

  override def findBpmModels: ServiceCall[BpmModelFindQuery, FindResult] =
    ServiceCall { query =>
      bpmModelService.findBpmModels(query)
    }

}
