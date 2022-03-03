package biz.lobachev.annette.camunda4s

import play.api.libs.ws.WSRequest

import scala.concurrent.Future

trait ApiExecutor[T] {

  def execute(request: WSRequest): Future[T]

}
