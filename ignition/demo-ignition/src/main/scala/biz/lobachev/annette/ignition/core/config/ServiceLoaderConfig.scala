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

trait ServiceLoaderConfig {
  val entities: Seq[String]
  val onError: ErrorMode
  val config: Config
}

object ServiceLoaderConfig {
  def entities(config: Config): Seq[String] =
    Try(
      config.getStringList("entities").asScala.toSeq
    )
      .getOrElse(Seq.empty)
}
