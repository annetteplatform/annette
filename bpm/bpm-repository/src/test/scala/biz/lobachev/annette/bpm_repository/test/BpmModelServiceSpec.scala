package biz.lobachev.annette.bpm_repository.test

import akka.Done
import biz.lobachev.annette.bpm_repository.api.domain.{BpmModelId, Notation}
import biz.lobachev.annette.bpm_repository.api.model.{
  BpmModel,
  BpmModelNotFound,
  CreateBpmModelPayload,
  DeleteBpmModelPayload,
  UpdateBpmModelDescriptionPayload,
  UpdateBpmModelNamePayload,
  UpdateBpmModelPayload,
  UpdateBpmModelXmlPayload
}
import biz.lobachev.annette.bpm_repository.impl.DBProvider
import biz.lobachev.annette.bpm_repository.impl.model.{BpmModelActions, BpmModelService}
import biz.lobachev.annette.core.exception.AnnetteTransportException
import biz.lobachev.annette.core.model.auth.PersonPrincipal
import io.scalaland.chimney.dsl._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import java.util.UUID
import scala.concurrent.ExecutionContext

class BpmModelServiceSpec extends AsyncWordSpecLike with Matchers {
  implicit val ec     = ExecutionContext.global
  val db              = DBProvider.databaseFactory("bpm-repository-db-test")
  val actions         = new BpmModelActions
  val bpmModelService = new BpmModelService(db, actions)

  "BpmModelService" should {

    "create model" in {
      val id      = BpmModelId(UUID.randomUUID().toString)
      val payload = CreateBpmModelPayload(
        id = id,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        bpmModel1 <- bpmModelService.createBpmModel(payload)
        bpmModel2 <- bpmModelService.getBpmModelById(id.value, true)

      } yield {
        bpmModelService.deleteBpmModel(DeleteBpmModelPayload(id, payload.updatedBy))
        val targetModel = payload
          .into[BpmModel]
          .withFieldConst(_.updatedAt, bpmModel1.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        bpmModel1 shouldBe targetModel
        bpmModel2 shouldBe targetModel
      }
    }

    "update model" in {
      val id            = BpmModelId(UUID.randomUUID().toString)
      val createPayload = CreateBpmModelPayload(
        id = id,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBpmModelPayload(
        id = id,
        name = "updated model name",
        description = "updated model description",
        notation = Notation.DMN,
        xml = BpmRepositoryData.dmnXml,
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        bpmModel0 <- bpmModelService.createBpmModel(createPayload)
        bpmModel1 <- bpmModelService.updateBpmModel(updatePayload)
        bpmModel2 <- bpmModelService.getBpmModelById(id.value, true)

      } yield {
        bpmModelService.deleteBpmModel(DeleteBpmModelPayload(id, createPayload.updatedBy))
        val targetModel0 = createPayload
          .into[BpmModel]
          .withFieldConst(_.updatedAt, bpmModel0.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        val targetModel1 = updatePayload
          .into[BpmModel]
          .withFieldConst(_.updatedAt, bpmModel1.updatedAt)
          .withFieldConst(_.code, "Decision_0ek7xn0")
          .transform
        bpmModel0 shouldBe targetModel0
        bpmModel1 shouldBe targetModel1
        bpmModel2 shouldBe targetModel1
      }
    }

    "update model name" in {
      val id            = BpmModelId(UUID.randomUUID().toString)
      val createPayload = CreateBpmModelPayload(
        id = id,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBpmModelNamePayload(
        id = id,
        name = "updated model name",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        bpmModel0 <- bpmModelService.createBpmModel(createPayload)
        bpmModel1 <- bpmModelService.updateBpmModelName(updatePayload)
        bpmModel2 <- bpmModelService.getBpmModelById(id.value, true)

      } yield {
        bpmModelService.deleteBpmModel(DeleteBpmModelPayload(id, createPayload.updatedBy))
        val targetModel0 = createPayload
          .into[BpmModel]
          .withFieldConst(_.updatedAt, bpmModel0.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        val targetModel1 = createPayload
          .into[BpmModel]
          .withFieldConst(_.name, updatePayload.name)
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, bpmModel1.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        bpmModel0 shouldBe targetModel0
        bpmModel1 shouldBe targetModel1
        bpmModel2 shouldBe targetModel1
      }
    }

    "update model description" in {
      val id            = BpmModelId(UUID.randomUUID().toString)
      val createPayload = CreateBpmModelPayload(
        id = id,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBpmModelDescriptionPayload(
        id = id,
        description = "updated model description",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        bpmModel0 <- bpmModelService.createBpmModel(createPayload)
        bpmModel1 <- bpmModelService.updateBpmModelDescription(updatePayload)
        bpmModel2 <- bpmModelService.getBpmModelById(id.value, true)

      } yield {
        bpmModelService.deleteBpmModel(DeleteBpmModelPayload(id, createPayload.updatedBy))
        val targetModel0 = createPayload
          .into[BpmModel]
          .withFieldConst(_.updatedAt, bpmModel0.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        val targetModel1 = createPayload
          .into[BpmModel]
          .withFieldConst(_.description, updatePayload.description)
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, bpmModel1.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        bpmModel0 shouldBe targetModel0
        bpmModel1 shouldBe targetModel1
        bpmModel2 shouldBe targetModel1
      }
    }

    "update model xml" in {
      val id            = BpmModelId(UUID.randomUUID().toString)
      val createPayload = CreateBpmModelPayload(
        id = id,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBpmModelXmlPayload(
        id = id,
        notation = Notation.DMN,
        xml = BpmRepositoryData.dmnXml,
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        bpmModel0 <- bpmModelService.createBpmModel(createPayload)
        bpmModel1 <- bpmModelService.updateBpmModelXml(updatePayload)
        bpmModel2 <- bpmModelService.getBpmModelById(id.value, true)

      } yield {
        bpmModelService.deleteBpmModel(DeleteBpmModelPayload(id, createPayload.updatedBy))
        val targetModel0 = createPayload
          .into[BpmModel]
          .withFieldConst(_.updatedAt, bpmModel0.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        val targetModel1 = updatePayload
          .into[BpmModel]
          .withFieldConst(_.name, bpmModel0.name)
          .withFieldConst(_.description, bpmModel0.description)
          .withFieldConst(_.updatedAt, bpmModel1.updatedAt)
          .withFieldConst(_.code, "Decision_0ek7xn0")
          .transform
        bpmModel0 shouldBe targetModel0
        bpmModel1 shouldBe targetModel1
        bpmModel2 shouldBe targetModel1
      }
    }

    "delete model" in {
      val id      = BpmModelId(UUID.randomUUID().toString)
      val payload = CreateBpmModelPayload(
        id = id,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        bpmModel1 <- bpmModelService.createBpmModel(payload)
        done      <- bpmModelService.deleteBpmModel(DeleteBpmModelPayload(id, payload.updatedBy))
        ex        <- recoverToExceptionIf[AnnetteTransportException](bpmModelService.getBpmModelById(id.value, true))

      } yield {
        val targetModel = payload
          .into[BpmModel]
          .withFieldConst(_.updatedAt, bpmModel1.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        bpmModel1 shouldBe targetModel
        done shouldBe Done
        ex shouldBe BpmModelNotFound(id.value)
      }
    }

    "get model by id with & w/o xml" in {
      val id      = BpmModelId(UUID.randomUUID().toString)
      val payload = CreateBpmModelPayload(
        id = id,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        bpmModel1 <- bpmModelService.createBpmModel(payload)
        bpmModel2 <- bpmModelService.getBpmModelById(id.value, true)
        bpmModel3 <- bpmModelService.getBpmModelById(id.value, false)

      } yield {
        bpmModelService.deleteBpmModel(DeleteBpmModelPayload(id, payload.updatedBy))
        val targetModel = payload
          .into[BpmModel]
          .withFieldConst(_.updatedAt, bpmModel1.updatedAt)
          .withFieldConst(_.code, "ApproveExpenses")
          .transform
        bpmModel1 shouldBe targetModel
        bpmModel2 shouldBe targetModel
        bpmModel3 shouldBe targetModel.copy(xml = None)
      }
    }

  }
}
