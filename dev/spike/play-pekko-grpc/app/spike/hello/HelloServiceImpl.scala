package spike.hello

import javax.inject._
import scala.concurrent.Future
import spike.hello.grpc._

/** Server-side implementation of the gRPC service. */
@Singleton
class HelloServiceImpl @Inject() () extends HelloService {
  override def sayHello(in: HelloRequest): Future[HelloReply] =
    Future.successful(HelloReply(s"Hello, ${in.name}! (from Play 3.0.x + Pekko gRPC)"))
}
