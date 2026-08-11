package spike.hello

import javax.inject._
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.grpc.scaladsl.ServerReflection
import pekko.http.scaladsl.Http
import play.api.inject.ApplicationLifecycle
import scala.concurrent.{ExecutionContext, Future}
import spike.hello.grpc._

/**
 * Wires the gRPC service into Play's lifecycle. Play 3.x already runs on Pekko HTTP with
 * HTTP/2 enabled; we bind an additional Pekko HTTP server on port 9001 for the gRPC service.
 *
 * (For a single-port deployment you would concat the gRPC handler with Play's routes via
 *  Play's `PekkoServerProvider`, but for the spike a separate port keeps the wiring minimal
 *  and proves both transports work side-by-side.)
 */
@Singleton
class GrpcServer @Inject() (
    impl: HelloServiceImpl,
    lifecycle: ApplicationLifecycle
)(implicit system: ActorSystem, ec: ExecutionContext) {

  private val handler = pekko.grpc.scaladsl.ServiceHandler.concatOrNotFound(
    HelloServiceHandler.partial(impl),
    ServerReflection.partial(List(HelloService))(system)
  )

  private val bound = Http().newServerAt("127.0.0.1", 9001).bind(handler)

  bound.foreach { sb =>
    println(s"[SPIKE-B] gRPC server bound to ${sb.localAddress}")
  }

  lifecycle.addStopHook { () =>
    bound.flatMap(_.unbind()).map(_ => ())
  }
}

/** Module to wire the GrpcServer into Play's lifecycle. */
class GrpcServerModule extends play.api.inject.SimpleModule(
  play.api.inject.bind[GrpcServer].toSelf.eagerly()
)
