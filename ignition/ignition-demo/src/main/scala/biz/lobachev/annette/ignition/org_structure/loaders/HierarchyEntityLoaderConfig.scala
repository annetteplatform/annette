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

package biz.lobachev.annette.ignition.org_structure.loaders

import biz.lobachev.annette.ignition.core.config.{EntityLoaderConfig, ErrorMode, LoadMode}
import com.typesafe.config.Config

import scala.jdk.CollectionConverters._
import scala.util.Try

case class HierarchyEntityLoaderConfig(
  data: Seq[String],
  onError: ErrorMode,
  mode: LoadMode,
  parallelism: Int,
  disposedCategory: String,
  removeDisposed: Boolean
) extends EntityLoaderConfig

object HierarchyEntityLoaderConfig {
  def apply(config: Config): HierarchyEntityLoaderConfig =
    HierarchyEntityLoaderConfig(
      data = Try(config.getStringList("data").asScala.toSeq).getOrElse(Seq.empty),
      onError = ErrorMode.fromConfig(config),
      mode = LoadMode.fromConfig(config),
      parallelism = Try(config.getInt("parallelism")).getOrElse(1),
      disposedCategory = Try(config.getString("disposed-category")).getOrElse("DISPOSED-UNIT"),
      removeDisposed = Try(config.getBoolean("remove-disposed")).getOrElse(false)
    )
}
