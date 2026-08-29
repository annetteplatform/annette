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

package biz.lobachev.annette.ignition.cms.loaders

import org.apache.pekko.Done
import org.apache.pekko.stream.Materializer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.core.model.category.{CreateCategoryPayload, UpdateCategoryPayload}
import biz.lobachev.annette.ignition.core.config.DefaultEntityLoaderConfig

import scala.concurrent.{ExecutionContext, Future}

class SpaceCategoryEntityLoader(
  service: CmsService,
  config: DefaultEntityLoaderConfig
)(implicit override val ec: ExecutionContext, override val materializer: Materializer)
    extends CmsCategoryEntityLoader(config, "space-category") {

  protected def createCategory(payload: CreateCategoryPayload): Future[Done] =
    service.createSpaceCategory(payload)

  protected def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    service.updateSpaceCategory(payload)

}
