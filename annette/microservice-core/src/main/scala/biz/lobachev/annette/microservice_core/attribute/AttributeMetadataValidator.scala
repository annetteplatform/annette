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

package biz.lobachev.annette.microservice_core.attribute

import biz.lobachev.annette.core.attribute.AttributeMetadata
import pureconfig._
import pureconfig.generic.auto._

trait AttributeMetadataValidator {
  def entity: String
  def configPath: String
  val metadata: Map[String, AttributeMetadata] = loadMetadata

  private def loadMetadata: Map[String, AttributeMetadata] =
    ConfigSource.default
      .at(configPath)
      .load[Map[String, AttributeConf]]
      .fold(
        failure => {
          val description = failure.toList.map(_.description).mkString(", ")
          throw AttributeConfigError(configPath, description)
        },
        config =>
          config.map {
            case (name, conf) =>
              name -> conf.toMetadata(name)
          }
      )

  def validateAttributes(attributes: Map[String, String]) = {
    attributes.map {
      case name -> value =>
        validateAttribute(name, value)
    }
    ()
  }

  def validateAttribute(name: String, value: String) = {
    val attributeMetadata = metadata.get(name).getOrElse(throw AttributeNotFound(entity, name))
    if (attributeMetadata.validate(value)) ()
    else throw InvalidAttribute(entity, name, value)
  }
}
