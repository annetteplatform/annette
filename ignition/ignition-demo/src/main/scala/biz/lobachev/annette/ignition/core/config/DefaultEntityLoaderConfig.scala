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

package biz.lobachev.annette.ignition.core.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters._
import scala.util.Try

case class DefaultEntityLoaderConfig(
  override val data: Seq[String],
  override val onError: ErrorMode,
  override val mode: LoadMode,
  override val parallelism: Int
) extends EntityLoaderConfig

object DefaultEntityLoaderConfig {
  def apply(config: Config): DefaultEntityLoaderConfig =
    DefaultEntityLoaderConfig(
      data = Try(config.getStringList("data").asScala.toSeq).getOrElse(Seq.empty),
      onError = ErrorMode.fromConfig(config),
      mode = LoadMode.fromConfig(config),
      parallelism = Try(config.getInt("parallelism")).getOrElse(1)
    )
}
