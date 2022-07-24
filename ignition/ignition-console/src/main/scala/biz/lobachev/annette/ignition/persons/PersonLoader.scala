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

package biz.lobachev.annette.ignition.persons

import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import biz.lobachev.annette.ignition.persons.loaders.{CategoryEntityLoader, PersonEntityLoader}
import biz.lobachev.annette.persons.api.{PersonServiceApi, PersonServiceImpl}
import com.softwaremill.macwire.wire

class PersonLoader(val client: IgnitionLagomClient, val config: PersonLoaderConfig)
    extends ServiceLoader[PersonLoaderConfig] {

  lazy val serviceApi = client.serviceClient.implement[PersonServiceApi]
  lazy val service    = wire[PersonServiceImpl]

  override def createEntityLoader(entity: String): EntityLoader[_, _] =
    entity match {
      case PersonLoader.Category => new CategoryEntityLoader(service, config.category.get)
      case PersonLoader.Person   => new PersonEntityLoader(service, config.person.get)
    }

  override val name: String = "person"
}

object PersonLoader {

  val Category = "category"
  val Person   = "person"

}
