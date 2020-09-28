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

package biz.lobachev.annette.core.discovery

import java.net.URI

import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryServiceLocator
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.client.CircuitBreakersPanel

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AnnetteDiscoveryServiceLocator(circuitBreakers: CircuitBreakersPanel, actorSystem: ActorSystem)(
  implicit
  ec: ExecutionContext
) extends AkkaDiscoveryServiceLocator(circuitBreakers, actorSystem)(ec) {
  val config = actorSystem.settings.config.getConfig("annette.discovery.services")
  override def locate(name: String, serviceCall: Descriptor.Call[_, _]): Future[Option[URI]] =
    Try { config.getString(name) }
      .map(uri => Future.successful(Some(new URI(uri))))
      .getOrElse(super.locate(name, serviceCall))

  override def locateAll(name: String, serviceCall: Descriptor.Call[_, _]): Future[List[URI]] =
    Try { config.getString(name) }
      .map(uri => Future.successful(List(new URI(uri))))
      .getOrElse(super.locateAll(name, serviceCall))

}
