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

import org.apache.pekko.stream.Materializer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.home_pages.AssignHomePagePayload
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, SystemPrincipal}
import biz.lobachev.annette.ignition.cms.loaders.data.HomePageData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.DefaultEntityLoaderConfig
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class HomePageEntityLoader(
  service: CmsService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[HomePageData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[HomePageData] = HomePageData.format

  override val name: String = "home-page"

  // assignHomePage is keyed by the composite id applicationId~principal and overwrites
  // an existing assignment, so re-running the loader is safe.
  def loadItem(item: HomePageData): Future[LoadStatus] =
    service
      .assignHomePage(
        AssignHomePagePayload(
          applicationId = item.applicationId,
          principal = AnnettePrincipal(item.principal),
          priority = item.priority.getOrElse(1),
          pageId = item.pageId,
          updatedBy = SystemPrincipal()
        )
      )
      .map(_ => LoadOk)
      .recover(th => LoadFailed(th.getMessage))

}
