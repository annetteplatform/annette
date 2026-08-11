package spike.hello

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

/** A regular Play controller — proves Play REST + Pekko gRPC coexist on the same server. */
@Singleton
class HelloController @Inject() (val controllerComponents: ControllerComponents)(implicit
    ec: ExecutionContext
) extends BaseController {
  def index(): Action[AnyContent] = Action.async { _ =>
    Future.successful(Ok("Play 3.0.x + Pekko gRPC spike: see /hello and grpc HelloService.SayHello"))
  }
}
