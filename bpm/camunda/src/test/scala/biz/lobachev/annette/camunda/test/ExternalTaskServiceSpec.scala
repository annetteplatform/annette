package biz.lobachev.annette.camunda.test

import akka.actor.ActorSystem
import biz.lobachev.annette.camunda.api._
import biz.lobachev.annette.camunda.api.external_task.{CompleteExternalTaskPayload, FetchAndLockQuery, TopicQuery}
import biz.lobachev.annette.camunda.api.repository.CreateDeploymentPayload
import biz.lobachev.annette.camunda.api.runtime.{DeleteProcessInstancePayload, StartProcessInstancePayload}
import biz.lobachev.annette.camunda.impl.{ExternalTaskServiceImpl, RepositoryServiceImpl, RuntimeServiceImpl}
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import play.api.libs.ws.ahc.{AhcWSClient, StandaloneAhcWSClient}

import scala.concurrent.ExecutionContext.global

class ExternalTaskServiceSpec extends AsyncWordSpecLike with Matchers {
  implicit val system    = ActorSystem()
  //  implicit val materializer = Materializer.create(actorContext)
  val standaloneWSClient = StandaloneAhcWSClient()
  val wsClient           = new AhcWSClient(standaloneWSClient)
  val config             = ConfigFactory.load()
  implicit val ec        = global

  val camundaClient     = CamundaFactory.createCamundaClient(config, wsClient)
  val service           = new ExternalTaskServiceImpl(camundaClient)
  val runtimeService    = new RuntimeServiceImpl(camundaClient)
  val repositoryService = new RepositoryServiceImpl(camundaClient)

  "ExternalTaskService" should {
    "fetchAndLock & complete" in {
      for {
        _     <- repositoryService.createDeployment(
                   CreateDeploymentPayload(
                     xml = BpmData.extTaskProcess,
                     deploymentName = Some("simple-process-ext"),
                     enableDuplicateFiltering = Some(false),
                     deployChangedOnly = None,
                     deploymentSource = Some("annette"),
                     deploymentActivationTime = None
                   )
                 )
        r1    <- runtimeService.startProcessInstanceByKey(
                   "SimpleProcessExtTask",
                   StartProcessInstancePayload(
                     variables = BpmData.variables,
                     withVariablesInReturn = Some(true)
                   )
                 )
        r2    <- service.fetchAndLockExternalTask(
                   FetchAndLockQuery(
                     workerId = "worker",
                     maxTasks = 10,
                     topics = Seq(
                       TopicQuery(
                         topicName = "TopicA",
                         lockDuration = 300000
                       )
                     )
                   )
                 )

        taskId = r2.filter(_.processInstanceId == r1.id).head.id
        _     <- service.completeExternalTask(taskId, CompleteExternalTaskPayload("worker"))
        _     <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r1)
        println(r2)
        r2.map(_.processInstanceId) should contain(r1.id)
      }
    }

  }
}
