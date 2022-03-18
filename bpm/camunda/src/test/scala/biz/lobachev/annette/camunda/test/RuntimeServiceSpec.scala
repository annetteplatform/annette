package biz.lobachev.annette.camunda.test

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.camunda.api._
import biz.lobachev.annette.camunda.api.common._
import biz.lobachev.annette.camunda.api.runtime.{
  DeleteProcessInstancePayload,
  ModifyProcessVariablePayload,
  ProcessInstanceFindQuery,
  StartProcessInstancePayload,
  SubmitStartFormPayload
}
import biz.lobachev.annette.camunda.impl.RuntimeServiceImpl
import biz.lobachev.annette.core.exception.AnnetteTransportException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import play.api.libs.ws.ahc.{AhcWSClient, StandaloneAhcWSClient}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class RuntimeServiceSpec extends AsyncWordSpecLike with Matchers {
  implicit val system    = ActorSystem()
  //  implicit val materializer = Materializer.create(actorContext)
  val standaloneWSClient = StandaloneAhcWSClient()
  val wsClient           = new AhcWSClient(standaloneWSClient)
  implicit val ec        = global

  val camundaClient = new CamundaClient("http://localhost:3090/engine-rest/engine/default", None, wsClient)
  val service       = new RuntimeServiceImpl(camundaClient)

  "ProcessInstance" should {

    "start process by id" in {
      for {
        r1 <- service.startProcessInstanceById(
                "SimpleProcess:2:ffa27d7e-a1ee-11ec-bea9-0242ac180003",
                StartProcessInstancePayload(
                  variables = BpmData.variables,
                  withVariablesInReturn = Some(true)
                )
              )
      } yield {
        println(r1)
        1 shouldBe 1
      }
    }

    "start process by key" in {
      for {
        r1 <- service.startProcessInstanceByKey(
                "SimpleProcess",
                StartProcessInstancePayload(
                  variables = BpmData.variables,
                  withVariablesInReturn = Some(true)
                )
              )
      } yield {
        println(r1)
        1 shouldBe 1
      }
    }

    "submit form by id" in {
      for {
        r1 <- service.submitStartFormById(
                "SimpleProcess:2:ffa27d7e-a1ee-11ec-bea9-0242ac180003",
                SubmitStartFormPayload(
                  variables = BpmData.variables
                )
              )
      } yield {
        println(r1)
        1 shouldBe 1
      }
    }

    "submit form by key" in {
      for {
        r1 <- service.submitStartFormByKey(
                "SimpleProcess",
                SubmitStartFormPayload(
                  variables = BpmData.variables
                )
              )
      } yield {
        println(r1)
        1 shouldBe 1
      }
    }

    "deleteProcessInstance" in {
      for {
        r1 <- service.startProcessInstanceByKey(
                "SimpleProcess",
                StartProcessInstancePayload(
                  variables = BpmData.variables
                )
              )
        r2 <- service.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
        ex <- recoverToExceptionIf[AnnetteTransportException](service.getProcessInstanceById(r1.id))
      } yield {
        r2 shouldBe Done
        ex.code shouldBe ProcessInstanceNotFound.MessageCode
      }
    }

    "deleteProcessInstance non-existing" in {
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](
                service.deleteProcessInstance(DeleteProcessInstancePayload("none"))
              )
      } yield ex.code shouldBe ProcessInstanceNotFound.MessageCode
    }

    "getProcessInstanceById" in {
      for {
        r1 <- service.startProcessInstanceByKey(
                "SimpleProcess",
                StartProcessInstancePayload(
                  variables = BpmData.variables
                )
              )
        r2 <- service.getProcessInstanceById(r1.id)
      } yield {
        println(r2)
        1 shouldBe 1
      }
    }

    "getProcessInstanceById non-existing" in {
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](service.getProcessInstanceById("none"))
      } yield {
        println(ex)
        ex.code shouldBe ProcessInstanceNotFound.MessageCode
      }
    }

    "findProcessInstances" in {
      for {
        res <- service.findProcessInstances(
                 ProcessInstanceFindQuery(variables = Some(Seq("str_eq_aStringValue1")))
               )
      } yield {
        println(s"total = ${res.total}")
        res.hits.foreach(println)
        1 shouldBe 1
      }
    }

    "remove created instances" in {
      val query = ProcessInstanceFindQuery(processDefinitionKey = Some("SimpleProcess"))
      for {
        r1     <- service.findProcessInstances(query)
        futures = r1.hits.map(r => service.deleteProcessInstance(DeleteProcessInstancePayload(r.id)))
        _      <- Future.sequence(futures)
        r3     <- service.findProcessInstances(query)
      } yield {
        println(s"total = ${r3.total}")
        r3.total shouldBe 0
      }
    }
  }

  "ProcessInstanceVariable" should {
    "modifyProcessVariables" in {
      val newValue = "newStringValue"
      for {
        r1 <- service.startProcessInstanceByKey(
                "SimpleProcess",
                StartProcessInstancePayload(
                  variables = BpmData.variables,
                  withVariablesInReturn = Some(true)
                )
              )
        _  <- service.modifyProcessVariables(
                r1.id,
                ModifyProcessVariablePayload(
                  modifications = Some(
                    Map(
                      "str" -> StringValue(newValue)
                    )
                  ),
                  deletions = Some(Seq("bool"))
                )
              )
        r3 <- service.getProcessVariables(r1.id)
        _  <- service.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3("str").asInstanceOf[StringValue].value shouldBe newValue
        r3.get("bool") shouldBe None
      }
    }

    "updateProcessVariable" in {
      val newValue = "newStringValue"
      for {
        r1 <- service.startProcessInstanceByKey(
                "SimpleProcess",
                StartProcessInstancePayload(
                  variables = BpmData.variables,
                  withVariablesInReturn = Some(true)
                )
              )
        _  <- service.updateProcessVariable(
                id = r1.id,
                varName = "str",
                value = StringValue(newValue)
              )
        r3 <- service.getProcessVariables(r1.id)
        _  <- service.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3("str").asInstanceOf[StringValue].value shouldBe newValue
      }
    }

    "deleteProcessVariable" in {
      for {
        r1 <- service.startProcessInstanceByKey(
                "SimpleProcess",
                StartProcessInstancePayload(
                  variables = BpmData.variables,
                  withVariablesInReturn = Some(true)
                )
              )
        _  <- service.deleteProcessVariable(
                id = r1.id,
                varName = "bool"
              )
        r3 <- service.getProcessVariables(r1.id)
        _  <- service.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3.get("bool") shouldBe None
      }
    }

    "getProcessVariable" in {
      val newValue = "newStringValue"
      for {
        r1     <- service.startProcessInstanceByKey(
                    "SimpleProcess",
                    StartProcessInstancePayload(
                      variables = BpmData.variables,
                      withVariablesInReturn = Some(true)
                    )
                  )
        _      <- service.modifyProcessVariables(
                    r1.id,
                    ModifyProcessVariablePayload(
                      modifications = Some(
                        Map(
                          "str" -> StringValue(newValue)
                        )
                      ),
                      deletions = Some(Seq("bool"))
                    )
                  )
        strVar <- service.getProcessVariable(r1.id, "str")
        boolEx <- recoverToExceptionIf[AnnetteTransportException](service.getProcessVariable(r1.id, "bool"))
        _      <- service.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        strVar.asInstanceOf[StringValue].value shouldBe newValue
        boolEx.code shouldBe ProcessInstanceVariableNotFound.MessageCode
      }
    }

    "remove created process instances" in {
      val query = ProcessInstanceFindQuery(processDefinitionKey = Some("SimpleProcess"))
      for {
        r1     <- service.findProcessInstances(query)
        futures = r1.hits.map(r => service.deleteProcessInstance(DeleteProcessInstancePayload(r.id)))
        _      <- Future.sequence(futures)
        r3     <- service.findProcessInstances(query)
      } yield {
        println(s"total = ${r3.total}")
        r3.total shouldBe 0
      }
    }
  }
}
