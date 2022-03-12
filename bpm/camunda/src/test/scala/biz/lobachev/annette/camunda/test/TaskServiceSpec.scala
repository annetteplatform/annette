package biz.lobachev.annette.camunda.test

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.camunda.api._
import biz.lobachev.annette.camunda.api.common.{StringValue, VariableValue}
import biz.lobachev.annette.camunda.api.runtime.{DeleteProcessInstancePayload, StartProcessInstancePayload}
import biz.lobachev.annette.camunda.api.task.{
  CompleteTaskPayload,
  CreateTaskPayload,
  ModifyTaskLocalVariablePayload,
  ModifyTaskVariablePayload,
  TaskFindQuery,
  UpdateTaskPayload
}
import biz.lobachev.annette.camunda.impl.{RuntimeServiceImpl, TaskServiceImpl}
import biz.lobachev.annette.core.exception.AnnetteTransportException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import play.api.libs.ws.ahc.{AhcWSClient, StandaloneAhcWSClient}

import java.util.UUID
import scala.concurrent.ExecutionContext.global

class TaskServiceSpec extends AsyncWordSpecLike with Matchers {
  implicit val system    = ActorSystem()
  //  implicit val materializer = Materializer.create(actorContext)
  val standaloneWSClient = StandaloneAhcWSClient()
  val wsClient           = new AhcWSClient(standaloneWSClient)
  implicit val ec        = global

  val camundaClient  = new CamundaClient("http://localhost:3090/engine-rest/engine/default", None, wsClient)
  val runtimeService = new RuntimeServiceImpl(camundaClient)
  val service        = new TaskServiceImpl(camundaClient)

  "Task" should {

    "createTask" in {
      val createTaskPayload = CreateTaskPayload(
        UUID.randomUUID().toString,
        "New Task"
      )
      for {
        _  <- service.createTask(createTaskPayload)
        r2 <- service.getTask(createTaskPayload.id)
        _  <- service.deleteTask(createTaskPayload.id)
      } yield r2.name shouldBe Some(createTaskPayload.name)
    }

    "updateTask" in {
      val createTaskPayload = CreateTaskPayload(
        UUID.randomUUID().toString,
        "New Task"
      )
      val updateTaskPayload = UpdateTaskPayload(
        Some("Updated Task")
      )
      for {
        _  <- service.createTask(createTaskPayload)
        _  <- service.updateTask(createTaskPayload.id, updateTaskPayload)
        r2 <- service.getTask(createTaskPayload.id)
        _  <- service.deleteTask(createTaskPayload.id)
      } yield r2.name shouldBe updateTaskPayload.name
    }

    "deleteTask" in {
      val createTaskPayload = CreateTaskPayload(
        UUID.randomUUID().toString,
        "New Task"
      )
      for {
        _  <- service.createTask(createTaskPayload)
        _  <- service.deleteTask(createTaskPayload.id)
        ex <- recoverToExceptionIf[AnnetteTransportException](service.getTask(createTaskPayload.id))
      } yield ex.code shouldBe TaskNotFound.MessageCode
    }

    "findTasks" in {
      for {
        r <- service.findTasks(TaskFindQuery())
      } yield {
        println(r.total)
        r.hits.foreach(println)
        1 shouldBe 1
      }
    }

    "claimTask/unclaimTask" in {
      val user1             = "user1"
      val user2             = "user2"
      val createTaskPayload = CreateTaskPayload(
        id = UUID.randomUUID().toString,
        name = "New Task",
        owner = Some(user1)
      )
      for {
        _  <- service.createTask(createTaskPayload)
        _  <- service.claimTask(createTaskPayload.id, user2)
        t1 <- service.getTask(createTaskPayload.id)
        _  <- service.unclaimTask(createTaskPayload.id)
        t2 <- service.getTask(createTaskPayload.id)
        _  <- service.deleteTask(createTaskPayload.id)
      } yield {
        t1.owner shouldBe Some(user1)
        t1.assignee shouldBe Some(user2)
        t2.owner shouldBe Some(user1)
        t2.assignee shouldBe None
      }
    }

    "setTaskAssignee" in {
      val user1             = "user1"
      val user2             = "user2"
      val createTaskPayload = CreateTaskPayload(
        id = UUID.randomUUID().toString,
        name = "New Task",
        owner = Some(user1)
      )
      for {
        _  <- service.createTask(createTaskPayload)
        _  <- service.setTaskAssignee(createTaskPayload.id, Some(user2))
        t1 <- service.getTask(createTaskPayload.id)
        _  <- service.setTaskAssignee(createTaskPayload.id, None)
        t2 <- service.getTask(createTaskPayload.id)
        _  <- service.deleteTask(createTaskPayload.id)
      } yield {
        t1.owner shouldBe Some(user1)
        t1.assignee shouldBe Some(user2)
        t2.owner shouldBe Some(user1)
        t2.assignee shouldBe None
      }
    }

    "delegateTask/resolveTask" in {
      val user1             = "user1"
      val user2             = "user2"
      val createTaskPayload = CreateTaskPayload(
        id = UUID.randomUUID().toString,
        name = "New Task",
        owner = Some(user1)
      )
      for {
        _  <- service.createTask(createTaskPayload)
        _  <- service.delegateTask(createTaskPayload.id, user2)
        t1 <- service.getTask(createTaskPayload.id)
        _  <- service.resolveTask(createTaskPayload.id)
        t2 <- service.getTask(createTaskPayload.id)
        _  <- service.deleteTask(createTaskPayload.id)
      } yield {
        t1.owner shouldBe Some(user1)
        t1.assignee shouldBe Some(user2)
        t1.delegationState shouldBe Some("PENDING")
        t2.owner shouldBe Some(user1)
        t2.assignee shouldBe Some(user1)
        t2.delegationState shouldBe Some("RESOLVED")
      }
    }

    "completeTask" in {
      val createTaskPayload = CreateTaskPayload(
        UUID.randomUUID().toString,
        "New Task"
      )
      for {
        _  <- service.createTask(createTaskPayload)
        r1 <- service.completeTask(createTaskPayload.id)
        ex <- recoverToExceptionIf[AnnetteTransportException](service.getTask(createTaskPayload.id))
      } yield {
        r1 shouldBe Left(Done)
        ex.code shouldBe TaskNotFound.MessageCode
      }
    }

    "completeTask with variables" in {
      val createTaskPayload = CreateTaskPayload(
        UUID.randomUUID().toString,
        "New Task"
      )
      for {
        _  <- service.createTask(createTaskPayload)
        r1 <- service.completeTask(createTaskPayload.id, CompleteTaskPayload(withVariablesInReturn = Some(true)))
        ex <- recoverToExceptionIf[AnnetteTransportException](service.getTask(createTaskPayload.id))
      } yield {
        r1 shouldBe Right(Map.empty[String, VariableValue])
        ex.code shouldBe TaskNotFound.MessageCode
      }
    }
  }

  "TaskVariable" should {
    "modifyTaskVariables" in {
      val newValue = "newStringValue"
      for {
        r1    <- runtimeService.startProcessInstanceByKey(
                   "SimpleProcess",
                   StartProcessInstancePayload(
                     variables = BpmData.variables,
                     withVariablesInReturn = Some(true)
                   )
                 )
        r2    <- service.findTasks(
                   TaskFindQuery(
                     processInstanceId = Some(r1.id)
                   )
                 )
        taskId = r2.hits.head.id
        _     <- service.modifyTaskVariables(
                   taskId,
                   ModifyTaskVariablePayload(
                     modifications = Some(
                       Map(
                         "str" -> StringValue(newValue)
                       )
                     ),
                     deletions = Some(Seq("bool"))
                   )
                 )
        r3    <- service.getTaskVariables(taskId)
        _     <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3("str").asInstanceOf[StringValue].value shouldBe newValue
        r3.get("bool") shouldBe None
      }
    }

    "updateTaskVariable" in {
      val newValue = "newStringValue"
      for {
        r1    <- runtimeService.startProcessInstanceByKey(
                   "SimpleProcess",
                   StartProcessInstancePayload(
                     variables = BpmData.variables,
                     withVariablesInReturn = Some(true)
                   )
                 )
        r2    <- service.findTasks(
                   TaskFindQuery(
                     processInstanceId = Some(r1.id)
                   )
                 )
        taskId = r2.hits.head.id
        _     <- service.updateTaskVariable(
                   id = taskId,
                   varName = "str",
                   value = StringValue(newValue)
                 )
        r3    <- service.getTaskVariables(taskId)
        _     <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3("str").asInstanceOf[StringValue].value shouldBe newValue
      }
    }

    "deleteTaskVariable" in {
      for {
        r1    <- runtimeService.startProcessInstanceByKey(
                   "SimpleProcess",
                   StartProcessInstancePayload(
                     variables = BpmData.variables,
                     withVariablesInReturn = Some(true)
                   )
                 )
        r2    <- service.findTasks(
                   TaskFindQuery(
                     processInstanceId = Some(r1.id)
                   )
                 )
        taskId = r2.hits.head.id
        _     <- service.deleteTaskVariable(
                   id = taskId,
                   varName = "bool"
                 )
        r3    <- service.getTaskVariables(taskId)
        _     <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3.get("bool") shouldBe None
      }
    }

    "getTaskVariable" in {
      val newValue = "newStringValue"
      for {
        r1     <- runtimeService.startProcessInstanceByKey(
                    "SimpleProcess",
                    StartProcessInstancePayload(
                      variables = BpmData.variables,
                      withVariablesInReturn = Some(true)
                    )
                  )
        r2     <- service.findTasks(
                    TaskFindQuery(
                      processInstanceId = Some(r1.id)
                    )
                  )
        taskId  = r2.hits.head.id
        _      <- service.modifyTaskVariables(
                    taskId,
                    ModifyTaskVariablePayload(
                      modifications = Some(
                        Map(
                          "str" -> StringValue(newValue)
                        )
                      ),
                      deletions = Some(Seq("bool"))
                    )
                  )
        strVar <- service.getTaskVariable(taskId, "str")
        boolEx <- recoverToExceptionIf[AnnetteTransportException](service.getTaskVariable(taskId, "bool"))
        _      <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        strVar.asInstanceOf[StringValue].value shouldBe newValue
        boolEx.code shouldBe TaskVariableNotFound.MessageCode
      }
    }

  }

  "TaskLocalVariable" should {
    "modifyTaskLocalVariables" in {
      val newValue = "newStringValue"
      for {
        r1    <- runtimeService.startProcessInstanceByKey(
                   "SimpleProcess",
                   StartProcessInstancePayload(
                     variables = BpmData.variables,
                     withVariablesInReturn = Some(true)
                   )
                 )
        r2    <- service.findTasks(
                   TaskFindQuery(
                     processInstanceId = Some(r1.id)
                   )
                 )
        taskId = r2.hits.head.id
        _     <- service.modifyTaskLocalVariables(
                   taskId,
                   ModifyTaskLocalVariablePayload(
                     modifications = Some(
                       Map(
                         "str" -> StringValue(newValue)
                       )
                     ),
                     deletions = Some(Seq("bool"))
                   )
                 )
        r3    <- service.getTaskLocalVariables(taskId)
        _     <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3("str").asInstanceOf[StringValue].value shouldBe newValue
        r3.get("bool") shouldBe None
      }
    }

    "updateTaskLocalVariable" in {
      val newValue = "newStringValue"
      for {
        r1    <- runtimeService.startProcessInstanceByKey(
                   "SimpleProcess",
                   StartProcessInstancePayload(
                     variables = BpmData.variables,
                     withVariablesInReturn = Some(true)
                   )
                 )
        r2    <- service.findTasks(
                   TaskFindQuery(
                     processInstanceId = Some(r1.id)
                   )
                 )
        taskId = r2.hits.head.id
        _     <- service.updateTaskLocalVariable(
                   id = taskId,
                   varName = "str",
                   value = StringValue(newValue)
                 )
        r3    <- service.getTaskLocalVariables(taskId)
        _     <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3("str").asInstanceOf[StringValue].value shouldBe newValue
      }
    }

    "deleteTaskLocalVariable" in {
      for {
        r1    <- runtimeService.startProcessInstanceByKey(
                   "SimpleProcess",
                   StartProcessInstancePayload(
                     variables = BpmData.variables,
                     withVariablesInReturn = Some(true)
                   )
                 )
        r2    <- service.findTasks(
                   TaskFindQuery(
                     processInstanceId = Some(r1.id)
                   )
                 )
        taskId = r2.hits.head.id
        _     <- service.deleteTaskLocalVariable(
                   id = taskId,
                   varName = "bool"
                 )
        r3    <- service.getTaskLocalVariables(taskId)
        _     <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        println(r3)
        r3.get("bool") shouldBe None
      }
    }

    "getTaskLocalVariable" in {
      val newValue = "newStringValue"
      for {
        r1     <- runtimeService.startProcessInstanceByKey(
                    "SimpleProcess",
                    StartProcessInstancePayload(
                      variables = BpmData.variables,
                      withVariablesInReturn = Some(true)
                    )
                  )
        r2     <- service.findTasks(
                    TaskFindQuery(
                      processInstanceId = Some(r1.id)
                    )
                  )
        taskId  = r2.hits.head.id
        _      <- service.modifyTaskLocalVariables(
                    taskId,
                    ModifyTaskLocalVariablePayload(
                      modifications = Some(
                        Map(
                          "str" -> StringValue(newValue)
                        )
                      ),
                      deletions = Some(Seq("bool"))
                    )
                  )
        strVar <- service.getTaskLocalVariable(taskId, "str")
        boolEx <- recoverToExceptionIf[AnnetteTransportException](service.getTaskLocalVariable(taskId, "bool"))
        _      <- runtimeService.deleteProcessInstance(DeleteProcessInstancePayload(r1.id))
      } yield {
        strVar.asInstanceOf[StringValue].value shouldBe newValue
        boolEx.code shouldBe TaskVariableNotFound.MessageCode
      }
    }
  }
}
