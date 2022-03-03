package biz.lobachev.annette.camunda4s.test

import akka.actor.ActorSystem
import biz.lobachev.annette.camunda4s.models.GetProcessDefinitionsRequest
import biz.lobachev.annette.camunda4s.{CamundaClient, ProcessDefinitionApi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.ws.ahc.{AhcWSClient, StandaloneAhcWSClient}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

class Camunda4sWSSpec extends AnyWordSpecLike with Matchers {
  implicit val system    = ActorSystem()
  //  implicit val materializer = Materializer.create(actorContext)
  val standaloneWSClient = StandaloneAhcWSClient()
  val wsClient           = new AhcWSClient(standaloneWSClient)
  implicit val ec        = global

  val camundaClent         = new CamundaClient("http://localhost:3090/engine-rest/engine/default", None, wsClient)
  val processDefinitionApi = new ProcessDefinitionApi(camundaClent)

  "Camunda4s" should {

    "run ws" in {
      val future = processDefinitionApi.getProcessDefinitions(
        GetProcessDefinitionsRequest(firstResult = Some(1), maxResults = Some(2))
      )

      val seq = Await.result(future, Duration.Inf)
      seq.foreach(println)

    }
  }
}
