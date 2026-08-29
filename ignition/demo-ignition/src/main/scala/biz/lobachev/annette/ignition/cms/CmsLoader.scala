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

package biz.lobachev.annette.ignition.cms

import com.typesafe.config.ConfigFactory
import biz.lobachev.annette.cms.api.{CmsServiceGrpcImpl, CmsStorage}
import biz.lobachev.annette.ignition.cms.loaders.{
  BlogCategoryEntityLoader,
  BlogEntityLoader,
  FileEntityLoader,
  HomePageEntityLoader,
  PageEntityLoader,
  PostEntityLoader,
  SpaceCategoryEntityLoader,
  SpaceEntityLoader
}
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, DefaultServiceLoaderConfig}
import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionGrpcClient, ServiceLoader}

class CmsLoader(
  val client: IgnitionGrpcClient,
  val config: DefaultServiceLoaderConfig
) extends ServiceLoader[DefaultServiceLoaderConfig] {

  lazy val service = new CmsServiceGrpcImpl(client.cmsGrpcClient)

  // Verifies/recreates the storage bucket at startup, mirroring the cms service itself.
  lazy val storage = new CmsStorage(
    ConfigFactory.load(),
    client.actorSystem,
    client.materializer,
    client.executionContext
  )

  override def createEntityLoader(entity: String): EntityLoader[_, _] =
    entity match {
      case CmsLoader.BlogCategory =>
        new BlogCategoryEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case CmsLoader.Blog         =>
        new BlogEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case CmsLoader.Post         =>
        new PostEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case CmsLoader.SpaceCategory =>
        new SpaceCategoryEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case CmsLoader.Space        =>
        new SpaceEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case CmsLoader.Page         =>
        new PageEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case CmsLoader.HomePage     =>
        new HomePageEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case CmsLoader.File         =>
        new FileEntityLoader(service, storage, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case _                      =>
        throw new IllegalArgumentException(s"Invalid entity: $entity ")
    }

  override val name: String = "cms"

}

object CmsLoader {

  val BlogCategory = "blog-category"
  val Blog         = "blog"
  val Post         = "post"
  val SpaceCategory = "space-category"
  val Space        = "space"
  val Page         = "page"
  val HomePage     = "home-page"
  val File         = "file"

}
