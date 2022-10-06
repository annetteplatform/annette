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

package biz.lobachev.annette.persons.impl

import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.persons.api.PersonServiceApi
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import play.api.LoggerConfigurator

class PersonServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new PersonServiceCasApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new PersonServiceCasApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[PersonServiceApi])
}
