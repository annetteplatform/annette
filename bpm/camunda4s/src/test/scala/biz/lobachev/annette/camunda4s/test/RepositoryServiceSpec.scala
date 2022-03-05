package biz.lobachev.annette.camunda4s.test

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.camunda4s.api.repository._
import biz.lobachev.annette.camunda4s.api.{
  CamundaClient,
  CreateDeploymentError,
  DeploymentNotFound,
  ProcessDefinitionNotFoundById,
  ProcessDefinitionNotFoundByKey
}
import biz.lobachev.annette.camunda4s.impl.RepositoryServiceImpl
import biz.lobachev.annette.core.exception.AnnetteTransportException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import play.api.libs.ws.ahc.{AhcWSClient, StandaloneAhcWSClient}

import scala.concurrent.ExecutionContext.global

class RepositoryServiceSpec extends AsyncWordSpecLike with Matchers {
  implicit val system    = ActorSystem()
  //  implicit val materializer = Materializer.create(actorContext)
  val standaloneWSClient = StandaloneAhcWSClient()
  val wsClient           = new AhcWSClient(standaloneWSClient)
  implicit val ec        = global

  val camundaClient = new CamundaClient("http://localhost:3090/engine-rest/engine/default", None, wsClient)
  val service       = new RepositoryServiceImpl(camundaClient)

  "RepositoryService" should {

    "createDeployment" in {
      for {
        r1 <- service.createDeployment(
                CreateDeploymentPayload(
                  xml = BpmData.processB,
                  deploymentName = Some("process-b"),
                  enableDuplicateFiltering = Some(false),
                  deployChangedOnly = None,
                  deploymentSource = Some("annette"),
                  deploymentActivationTime = None
                )
              )
        _  <- service.deleteDeployment(DeleteDeploymentPayload(r1.id))
      } yield {
        println(r1)
        1 shouldBe 1
      }
    }

    "createDeployment with error" in {
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](
                service.createDeployment(
                  CreateDeploymentPayload(
                    xml = BpmData.processBe,
                    deploymentName = Some("process-b-e"),
                    enableDuplicateFiltering = Some(false),
                    deployChangedOnly = None,
                    deploymentSource = Some("annette"),
                    deploymentActivationTime = None
                  )
                )
              )
      } yield {
        println(ex.getMessage)
        ex.code shouldBe CreateDeploymentError.MessageCode
      }
    }

    "deleteDeployment" in {
      for {
        r1 <- service.createDeployment(
                CreateDeploymentPayload(
                  xml = BpmData.processB,
                  deploymentName = Some("process-b"),
                  enableDuplicateFiltering = Some(false),
                  deployChangedOnly = None,
                  deploymentSource = Some("annette"),
                  deploymentActivationTime = None
                )
              )
        r2 <- service.deleteDeployment(DeleteDeploymentPayload(r1.id))
        ex <- recoverToExceptionIf[AnnetteTransportException](service.getDeploymentById(r1.id))
      } yield {
        r2 shouldBe Done
        ex.code shouldBe DeploymentNotFound.MessageCode
      }
    }

    "deleteDeployment non-existing" in {
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](service.deleteDeployment(DeleteDeploymentPayload("none")))
      } yield ex.code shouldBe DeploymentNotFound.MessageCode
    }

    "getDeploymentById" in {
      for {
        r1 <- service.createDeployment(
                CreateDeploymentPayload(
                  xml = BpmData.processB,
                  deploymentName = Some("process-b"),
                  enableDuplicateFiltering = Some(false),
                  deployChangedOnly = None,
                  deploymentSource = Some("annette"),
                  deploymentActivationTime = None
                )
              )
        r2 <- service.getDeploymentById(r1.id)
        _  <- service.deleteDeployment(DeleteDeploymentPayload(r1.id))
      } yield {
        println(r2)
        1 shouldBe 1
      }
    }

    "getDeploymentById non-existing" in {
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](service.getDeploymentById("none"))
      } yield {
        println(ex)
        ex.code shouldBe DeploymentNotFound.MessageCode
      }
    }

    "findDeployments" in {
      for {
        res <- service.findDeployments(
                 DeploymentFindQuery( /*firstResult = Some(1), maxResults = Some(2)*/ )
               )
      } yield {
        println(s"total = ${res.total}")
        res.hits.foreach(println)
        1 shouldBe 1
      }
    }

    "deleteProcessDefinition" in {
      for {
        r1 <- service.createDeployment(
                CreateDeploymentPayload(
                  xml = BpmData.processB,
                  deploymentName = Some("process-b"),
                  enableDuplicateFiltering = Some(false),
                  deployChangedOnly = None,
                  deploymentSource = Some("annette"),
                  deploymentActivationTime = None
                )
              )
        r2 <-
          service.deleteProcessDefinition(DeleteProcessDefinitionPayload(r1.deployedProcessDefinitions.get.keys.head))
      } yield {
        println(r2)
        1 shouldBe 1
      }
    }

    "deleteProcessDefinition non-existing" in {
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](
                service.deleteProcessDefinition(DeleteProcessDefinitionPayload("none"))
              )
      } yield {
        println(ex)
        ex.code shouldBe ProcessDefinitionNotFoundById.MessageCode
      }

    }

    "getProcessDefinitionById" in {
      for {
        r1 <- service.createDeployment(
                CreateDeploymentPayload(
                  xml = BpmData.processB,
                  deploymentName = Some("process-b"),
                  enableDuplicateFiltering = Some(false),
                  deployChangedOnly = None,
                  deploymentSource = Some("annette"),
                  deploymentActivationTime = None
                )
              )
        r2 <- service.getProcessDefinitionById(r1.deployedProcessDefinitions.get.values.head.id)
        _  <- service.deleteDeployment(DeleteDeploymentPayload(r1.id))
      } yield {
        println(r2)
        1 shouldBe 1
      }
    }

    "getProcessDefinitionById non-existing" in {
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](service.getProcessDefinitionById("none"))
      } yield {
        println(ex)
        ex.code shouldBe ProcessDefinitionNotFoundById.MessageCode
      }
    }

    "getProcessDefinitionByKey" in {
      for {
        r1 <- service.createDeployment(
                CreateDeploymentPayload(
                  xml = BpmData.processB,
                  deploymentName = Some("process-b"),
                  enableDuplicateFiltering = Some(false),
                  deployChangedOnly = None,
                  deploymentSource = Some("annette"),
                  deploymentActivationTime = None
                )
              )
        r2 <- service.getProcessDefinitionByKey(r1.deployedProcessDefinitions.get.values.head.key)
        _  <- service.deleteDeployment(DeleteDeploymentPayload(r1.id))
      } yield {
        println(r2)
        1 shouldBe 1
      }
    }

    "getProcessDefinitionByKey non-existing" in {
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](
                service.getProcessDefinitionByKey("none")
              )
      } yield {
        println(ex)
        ex.code shouldBe ProcessDefinitionNotFoundByKey.MessageCode
      }
    }

    "findProcessDefinitions" in {
      for {
        res <- service.findProcessDefinitions(
                 ProcessDefinitionFindQuery( /*firstResult = Some(1), maxResults = Some(2)*/ )
               )
      } yield {
        println(s"total = ${res.total}")
        res.hits.foreach(println)
        1 shouldBe 1
      }
    }
  }
}
