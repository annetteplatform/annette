package biz.lobachev.annette.camunda.test

import akka.actor.ActorSystem
import biz.lobachev.annette.camunda.api._
import biz.lobachev.annette.camunda.api.task.{CreateTaskPayload, TaskFindQuery, UpdateTaskPayload}
import biz.lobachev.annette.camunda.impl.TaskServiceImpl
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

  val camundaClient = new CamundaClient("http://localhost:3090/engine-rest/engine/default", None, wsClient)
  val service       = new TaskServiceImpl(camundaClient)

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

  }
}
