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

package biz.lobachev.annette.ignition.core

import org.apache.pekko.Done
import biz.lobachev.annette.application.api.grpc.ApplicationServiceClient
import biz.lobachev.annette.authorization.api.grpc.AuthorizationServiceClient
import biz.lobachev.annette.cms.api.grpc.CmsServiceClient
import biz.lobachev.annette.org_structure.api.grpc.OrgStructureServiceClient
import biz.lobachev.annette.persons.api.grpc.PersonServiceClient
import biz.lobachev.annette.principal_group.api.grpc.PrincipalGroupServiceClient
import biz.lobachev.annette.service_catalog.api.grpc.ServiceCatalogServiceClient
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.grpc.GrpcClientSettings
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SystemMaterializer
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.{ExecutionContext, Future}

/**
 * Pekko gRPC variant of the ignition client (slice 013). Replaces the former
 * `IgnitionGrpcClient` (StandaloneLagomClientFactory): each backend service is a
 * generated Pekko gRPC client built from the `pekko.grpc.client.<name>` config blocks
 * (see conf/ignition.conf). The Keycloak seeding stage keeps using a standalone Play
 * WS client.
 */
class IgnitionGrpcClient() {

  val config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = ActorSystem("ignition")

  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  implicit val materializer: Materializer = SystemMaterializer(actorSystem).materializer

  val wsClient: StandaloneAhcWSClient = StandaloneAhcWSClient()

  private def clientSettings(name: String): GrpcClientSettings =
    GrpcClientSettings.fromConfig(name)

  lazy val authorizationGrpcClient   = AuthorizationServiceClient(clientSettings("authorization"))(actorSystem)
  lazy val personGrpcClient          = PersonServiceClient(clientSettings("persons"))(actorSystem)
  lazy val principalGroupGrpcClient  = PrincipalGroupServiceClient(clientSettings("principal-groups"))(actorSystem)
  lazy val orgStructureGrpcClient    = OrgStructureServiceClient(clientSettings("org-structure"))(actorSystem)
  lazy val serviceCatalogGrpcClient  = ServiceCatalogServiceClient(clientSettings("service-catalog"))(actorSystem)
  lazy val applicationGrpcClient     = ApplicationServiceClient(clientSettings("application"))(actorSystem)
  lazy val cmsGrpcClient             = CmsServiceClient(clientSettings("cms"))(actorSystem)

  // Play 3's WSClient.close() returns Unit (AutoCloseable).
  def close(): Future[Done] = {
    wsClient.close()
    actorSystem.terminate().map(_ => Done)
  }
}
