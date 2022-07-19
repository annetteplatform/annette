package biz.lobachev.annette.ignition.core

import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import com.lightbend.lagom.scaladsl.client.StandaloneLagomClientFactory
import play.api.libs.ws.ahc.AhcWSComponents

class IgnitionLagomClient
    extends StandaloneLagomClientFactory("ConsoleIgnitionServiceClient")
    with AhcWSComponents
    with AnnetteDiscoveryComponents {}
