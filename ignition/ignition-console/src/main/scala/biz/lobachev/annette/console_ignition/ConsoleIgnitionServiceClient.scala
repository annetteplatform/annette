package biz.lobachev.annette.console_ignition

import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import com.lightbend.lagom.scaladsl.client.StandaloneLagomClientFactory
import play.api.libs.ws.ahc.AhcWSComponents

class ConsoleIgnitionServiceClient
    extends StandaloneLagomClientFactory("ConsoleIgnitionServiceClient")
    with AhcWSComponents
    with AnnetteDiscoveryComponents {}
