package biz.lobachev.annette.bpm_repository.test

import akka.Done
import biz.lobachev.annette.bpm_repository.api.bp._
import biz.lobachev.annette.bpm_repository.api.domain.{
  BpmModelId,
  BusinessProcessId,
  DataSchemaId,
  Datatype,
  Notation,
  ProcessDefinitionId,
  VariableName
}
import biz.lobachev.annette.bpm_repository.api.model.{BpmModelNotFound, CreateBpmModelPayload, DeleteBpmModelPayload}
import biz.lobachev.annette.bpm_repository.api.schema.{
  CreateDataSchemaPayload,
  DataSchemaNotFound,
  DataSchemaVariable,
  DeleteDataSchemaPayload
}
import biz.lobachev.annette.bpm_repository.impl.DBProvider
import biz.lobachev.annette.bpm_repository.impl.bp.{BusinessProcessActions, BusinessProcessService}
import biz.lobachev.annette.bpm_repository.impl.model.{BpmModelActions, BpmModelService}
import biz.lobachev.annette.bpm_repository.impl.schema.{DataSchemaActions, DataSchemaService}
import biz.lobachev.annette.core.exception.AnnetteTransportException
import biz.lobachev.annette.core.model.auth.PersonPrincipal
import io.scalaland.chimney.dsl._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import java.util.UUID
import scala.concurrent.ExecutionContext

class BusinessProcessServiceSpec extends AsyncWordSpecLike with Matchers {
  implicit val ec            = ExecutionContext.global
  val db                     = DBProvider.databaseFactory("bpm-repository-db-test")
  val bpmModelActions        = new BpmModelActions
  val bpmModelService        = new BpmModelService(db, bpmModelActions)
  val dataSchemaActions      = new DataSchemaActions
  val dataSchemaService      = new DataSchemaService(db, dataSchemaActions)
  val businessProcessActions = new BusinessProcessActions
  val businessProcessService = new BusinessProcessService(db, businessProcessActions)

  "BusinessProcessService" should {

    "create business process" in {
      val id      = BusinessProcessId(UUID.randomUUID().toString)
      val payload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        businessProcess1 <- businessProcessService.createBusinessProcess(payload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)
      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, payload.updatedBy))
        val targetSchema = payload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess1 shouldBe targetSchema
        businessProcess2 shouldBe targetSchema
      }
    }

    "create business process with data schema" in {
      val id                = BusinessProcessId(UUID.randomUUID().toString)
      val dataSchemaId      = DataSchemaId(UUID.randomUUID().toString)
      val dataSchemaPayload = CreateDataSchemaPayload(
        id = dataSchemaId,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val payload           = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        dataSchemaId = Some(dataSchemaId),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var4" -> BusinessProcessVariable("var4", "var4", Datatype.Integer, "123"),
          "var5" -> BusinessProcessVariable("var5", "var5", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        _ <- dataSchemaService.createDataSchema(dataSchemaPayload)
        businessProcess1 <- businessProcessService.createBusinessProcess(payload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)
      } yield {
        businessProcessService
          .deleteBusinessProcess(DeleteBusinessProcessPayload(id, payload.updatedBy))
          .andThen(_ => dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(dataSchemaId, payload.updatedBy)))
        val targetSchema = BusinessProcess(
          id = id,
          name = "business process name",
          description = "business process description",
          dataSchemaId = Some(dataSchemaId),
          variables = Map(
            "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
            "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
            "var4" -> BusinessProcessVariable("var4", "var4", Datatype.Integer, "123"),
            "var5" -> BusinessProcessVariable("var5", "var5", Datatype.Boolean, "true")
          ),
          updatedBy = PersonPrincipal("P0001"),
          updatedAt = businessProcess1.updatedAt
        )
        businessProcess1 shouldBe targetSchema
        businessProcess2 shouldBe targetSchema
      }
    }

    "create business process with non-existing data schema" in {
      val id           = BusinessProcessId(UUID.randomUUID().toString)
      val dataSchemaId = DataSchemaId(UUID.randomUUID().toString)
      val payload      = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        dataSchemaId = Some(dataSchemaId),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var4" -> BusinessProcessVariable("var4", "var4", Datatype.Integer, "123"),
          "var5" -> BusinessProcessVariable("var5", "var5", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](businessProcessService.createBusinessProcess(payload))
      } yield ex shouldBe DataSchemaNotFound(dataSchemaId.value)
    }

    "create business process with bpm model" in {
      val id              = BusinessProcessId(UUID.randomUUID().toString)
      val bpmModelId      = BpmModelId(UUID.randomUUID().toString)
      val bpmModelPayload = CreateBpmModelPayload(
        id = bpmModelId,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      val payload         = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        bpmModelId = Some(bpmModelId),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var4" -> BusinessProcessVariable("var4", "var4", Datatype.Integer, "123"),
          "var5" -> BusinessProcessVariable("var5", "var5", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        _ <- bpmModelService.createBpmModel(bpmModelPayload)
        businessProcess1 <- businessProcessService.createBusinessProcess(payload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)
      } yield {
        businessProcessService
          .deleteBusinessProcess(DeleteBusinessProcessPayload(id, payload.updatedBy))
          .andThen(_ => bpmModelService.deleteBpmModel(DeleteBpmModelPayload(bpmModelId, payload.updatedBy)))

        val targetSchema = BusinessProcess(
          id = id,
          name = "business process name",
          description = "business process description",
          bpmModelId = Some(bpmModelId),
          variables = Map(
            "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
            "var4" -> BusinessProcessVariable("var4", "var4", Datatype.Integer, "123"),
            "var5" -> BusinessProcessVariable("var5", "var5", Datatype.Boolean, "true")
          ),
          updatedBy = PersonPrincipal("P0001"),
          updatedAt = businessProcess1.updatedAt
        )
        businessProcess1 shouldBe targetSchema
        businessProcess2 shouldBe targetSchema
      }
    }

    "create business process with non-existing bpm model" in {
      val id         = BusinessProcessId(UUID.randomUUID().toString)
      val bpmModelId = BpmModelId(UUID.randomUUID().toString)
      val payload    = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        bpmModelId = Some(bpmModelId),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var4" -> BusinessProcessVariable("var4", "var4", Datatype.Integer, "123"),
          "var5" -> BusinessProcessVariable("var5", "var5", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](businessProcessService.createBusinessProcess(payload))
      } yield ex shouldBe BpmModelNotFound(bpmModelId.value)
    }

    "update business process" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBusinessProcessPayload(
        id = id,
        name = "updated business process name",
        description = "updated business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hi"),
          "int2" -> BusinessProcessVariable("int2", "int2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "false"),
          "json" -> BusinessProcessVariable("json", "json", Datatype.Json, "{\"principalType\": \"person\"}")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        businessProcess0 <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.updateBusinessProcess(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)

      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess0.updatedAt)
          .transform
        val targetSchema1 = updatePayload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess0 shouldBe targetSchema0
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "update business process with data schema" in {
      val id                = BusinessProcessId(UUID.randomUUID().toString)
      val dataSchemaId      = DataSchemaId(UUID.randomUUID().toString)
      val dataSchemaPayload = CreateDataSchemaPayload(
        id = dataSchemaId,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val createPayload     = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload     = UpdateBusinessProcessPayload(
        id = id,
        name = "updated business process name",
        description = "updated business process description",
        dataSchemaId = Some(dataSchemaId),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hi"),
          "int2" -> BusinessProcessVariable("int2", "int2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "false"),
          "json" -> BusinessProcessVariable("json", "json", Datatype.Json, "{\"principalType\": \"person\"}")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        _ <- dataSchemaService.createDataSchema(dataSchemaPayload)
        _                <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.updateBusinessProcess(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)
      } yield {
        businessProcessService
          .deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
          .andThen(_ =>
            dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(dataSchemaId, updatePayload.updatedBy))
          )

        val targetSchema1 = BusinessProcess(
          id = id,
          name = "updated business process name",
          description = "updated business process description",
          dataSchemaId = Some(dataSchemaId),
          variables = Map(
            "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
            "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hi"),
            "int2" -> BusinessProcessVariable("int2", "int2", Datatype.Integer, "123"),
            "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "false"),
            "json" -> BusinessProcessVariable("json", "json", Datatype.Json, "{\"principalType\": \"person\"}")
          ),
          updatedBy = PersonPrincipal("P0001"),
          updatedAt = businessProcess1.updatedAt
        )
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "update business process with non-existing data schema" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val dataSchemaId  = DataSchemaId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBusinessProcessPayload(
        id = id,
        name = "updated business process name",
        description = "updated business process description",
        dataSchemaId = Some(dataSchemaId),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hi"),
          "int2" -> BusinessProcessVariable("int2", "int2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "false"),
          "json" -> BusinessProcessVariable("json", "json", Datatype.Json, "{\"principalType\": \"person\"}")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        _ <- businessProcessService.createBusinessProcess(createPayload)
        ex <-
          recoverToExceptionIf[AnnetteTransportException](businessProcessService.updateBusinessProcess(updatePayload))
      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        ex shouldBe DataSchemaNotFound(dataSchemaId.value)
      }
    }

    "update business process with bpm model" in {
      val id              = BusinessProcessId(UUID.randomUUID().toString)
      val bpmModelId      = BpmModelId(UUID.randomUUID().toString)
      val bpmModelPayload = CreateBpmModelPayload(
        id = bpmModelId,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      val createPayload   = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload   = UpdateBusinessProcessPayload(
        id = id,
        name = "updated business process name",
        description = "updated business process description",
        bpmModelId = Some(bpmModelId),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hi"),
          "int2" -> BusinessProcessVariable("int2", "int2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "false"),
          "json" -> BusinessProcessVariable("json", "json", Datatype.Json, "{\"principalType\": \"person\"}")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        _ <- bpmModelService.createBpmModel(bpmModelPayload)
        _                <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.updateBusinessProcess(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)
      } yield {
        businessProcessService
          .deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
          .andThen(_ => bpmModelService.deleteBpmModel(DeleteBpmModelPayload(bpmModelId, createPayload.updatedBy)))
        val targetSchema1 = BusinessProcess(
          id = id,
          name = "updated business process name",
          description = "updated business process description",
          bpmModelId = Some(bpmModelId),
          variables = Map(
            "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hi"),
            "int2" -> BusinessProcessVariable("int2", "int2", Datatype.Integer, "123"),
            "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "false"),
            "json" -> BusinessProcessVariable("json", "json", Datatype.Json, "{\"principalType\": \"person\"}")
          ),
          updatedBy = PersonPrincipal("P0001"),
          updatedAt = businessProcess1.updatedAt
        )
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "update business process with non-existing bpm model" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val bpmModelId    = BpmModelId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBusinessProcessPayload(
        id = id,
        name = "updated business process name",
        description = "updated business process description",
        bpmModelId = Some(bpmModelId),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hi"),
          "int2" -> BusinessProcessVariable("int2", "int2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "false"),
          "json" -> BusinessProcessVariable("json", "json", Datatype.Json, "{\"principalType\": \"person\"}")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        _ <- businessProcessService.createBusinessProcess(createPayload)
        ex <-
          recoverToExceptionIf[AnnetteTransportException](businessProcessService.updateBusinessProcess(updatePayload))
      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        ex shouldBe BpmModelNotFound(bpmModelId.value)
      }
    }

    "update name" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBusinessProcessNamePayload(
        id = id,
        name = "updated business process name",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        businessProcess0 <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.updateBusinessProcessName(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)

      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.name, updatePayload.name)
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess0 shouldBe targetSchema0
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "update description" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBusinessProcessDescriptionPayload(
        id = id,
        description = "updated business process description",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        businessProcess0 <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.updateBusinessProcessDescription(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)

      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.description, updatePayload.description)
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess0 shouldBe targetSchema0
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "update data schema" in {
      val id                = BusinessProcessId(UUID.randomUUID().toString)
      val dataSchemaId      = DataSchemaId(UUID.randomUUID().toString)
      val dataSchemaPayload = CreateDataSchemaPayload(
        id = dataSchemaId,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var4" -> DataSchemaVariable("var4", "var4", Datatype.Integer, "123")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val createPayload     = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload     = UpdateBusinessProcessDataSchemaPayload(
        id = id,
        dataSchemaId = Some(dataSchemaId),
        updatedBy = PersonPrincipal("P0002")
      )
      val updatePayload2    = UpdateBusinessProcessDataSchemaPayload(
        id = id,
        dataSchemaId = None,
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        _                <- dataSchemaService.createDataSchema(dataSchemaPayload)
        _                <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.updateBusinessProcessDataSchema(updatePayload)
        businessProcess2 <- businessProcessService.updateBusinessProcessDataSchema(updatePayload2)
      } yield {
        businessProcessService
          .deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
          .andThen(_ =>
            dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(dataSchemaId, updatePayload.updatedBy))
          )
        val targetSchema1 = BusinessProcess(
          id = id,
          name = "business process name",
          description = "business process description",
          dataSchemaId = Some(dataSchemaId),
          variables = Map(
            "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true"),
            "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
            "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
            "var4" -> BusinessProcessVariable("var4", "var4", Datatype.Integer, "123")
          ),
          updatedBy = PersonPrincipal("P0002"),
          updatedAt = businessProcess1.updatedAt
        )
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1.copy(dataSchemaId = None, updatedAt = businessProcess2.updatedAt)
      }
    }

    "update with non-existing data schema" in {
      val id           = BusinessProcessId(UUID.randomUUID().toString)
      val dataSchemaId = DataSchemaId(UUID.randomUUID().toString)

      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBusinessProcessDataSchemaPayload(
        id = id,
        dataSchemaId = Some(dataSchemaId),
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        _  <- businessProcessService.createBusinessProcess(createPayload)
        ex <- recoverToExceptionIf[AnnetteTransportException](
                businessProcessService.updateBusinessProcessDataSchema(updatePayload)
              )
      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        ex shouldBe DataSchemaNotFound(dataSchemaId.value)
      }
    }

    "update bpm model" in {
      val id              = BusinessProcessId(UUID.randomUUID().toString)
      val bpmModelId      = BpmModelId(UUID.randomUUID().toString)
      val bpmModelPayload = CreateBpmModelPayload(
        id = bpmModelId,
        name = "model name",
        description = "model description",
        notation = Notation.BPMN,
        xml = BpmRepositoryData.bpmnXml,
        updatedBy = PersonPrincipal("P0001")
      )
      val createPayload   = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload   = UpdateBusinessProcessBpmModelPayload(
        id = id,
        bpmModelId = Some(bpmModelId),
        updatedBy = PersonPrincipal("P0002")
      )
      val updatePayload2  = UpdateBusinessProcessBpmModelPayload(
        id = id,
        bpmModelId = None,
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        _                <- bpmModelService.createBpmModel(bpmModelPayload)
        _                <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.updateBusinessProcessBpmModel(updatePayload)
        businessProcess2 <- businessProcessService.updateBusinessProcessBpmModel(updatePayload2)

      } yield {
        businessProcessService
          .deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
          .andThen(_ => bpmModelService.deleteBpmModel(DeleteBpmModelPayload(bpmModelId, updatePayload.updatedBy)))
        val targetSchema1 = BusinessProcess(
          id = id,
          name = "business process name",
          description = "business process description",
          bpmModelId = Some(bpmModelId),
          variables = Map(
            "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
            "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
            "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
          ),
          updatedBy = PersonPrincipal("P0002"),
          updatedAt = businessProcess1.updatedAt
        )
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1.copy(bpmModelId = None, updatedAt = businessProcess2.updatedAt)
      }
    }

    "update with non-existing bpm model" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val bpmModelId    = BpmModelId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateBusinessProcessBpmModelPayload(
        id = id,
        bpmModelId = Some(bpmModelId),
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        _  <- businessProcessService.createBusinessProcess(createPayload)
        ex <- recoverToExceptionIf[AnnetteTransportException](
                businessProcessService.updateBusinessProcessBpmModel(updatePayload)
              )

      } yield {
        businessProcessService
          .deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        ex shouldBe BpmModelNotFound(bpmModelId.value)
      }
    }

    "update process definition" in {
      val id             = BusinessProcessId(UUID.randomUUID().toString)
      val createPayload  = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload  = UpdateBusinessProcessProcessDefinitionPayload(
        id = id,
        processDefinitionId = Some(ProcessDefinitionId("process definition")),
        updatedBy = PersonPrincipal("P0002")
      )
      val updatePayload2 = UpdateBusinessProcessProcessDefinitionPayload(
        id = id,
        processDefinitionId = None,
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        _                <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.updateBusinessProcessProcessDefinition(updatePayload)
        businessProcess2 <- businessProcessService.updateBusinessProcessProcessDefinition(updatePayload2)

      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        val targetSchema1 = BusinessProcess(
          id = id,
          name = "business process name",
          description = "business process description",
          processDefinitionId = Some(ProcessDefinitionId("process definition")),
          variables = Map(
            "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
            "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
            "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
          ),
          updatedBy = PersonPrincipal("P0002"),
          updatedAt = businessProcess1.updatedAt
        )
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1.copy(processDefinitionId = None, updatedAt = businessProcess2.updatedAt)
      }
    }

    "add variable" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = StoreBusinessProcessVariablePayload(
        businessProcessId = id,
        variableName = VariableName("newVar"),
        name = "newVar",
        caption = "newVar",
        datatype = Datatype.Xml,
        defaultValue = "<name>Valery</name>",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        businessProcess0 <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.storeBusinessProcessVariable(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)

      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[BusinessProcess]
          .withFieldComputed(
            _.variables,
            _.variables + (updatePayload.variableName.value -> updatePayload.transformInto[BusinessProcessVariable])
          )
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess0 shouldBe targetSchema0
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "change variable" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = StoreBusinessProcessVariablePayload(
        businessProcessId = id,
        variableName = VariableName("var3"),
        name = "newVar",
        caption = "newVar",
        datatype = Datatype.Xml,
        defaultValue = "<name>Valery</name>",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        businessProcess0 <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.storeBusinessProcessVariable(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)

      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[BusinessProcess]
          .withFieldComputed(
            _.variables,
            _.variables + (updatePayload.variableName.value -> updatePayload.transformInto[BusinessProcessVariable])
          )
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess0 shouldBe targetSchema0
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "change variable name" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = StoreBusinessProcessVariablePayload(
        businessProcessId = id,
        variableName = VariableName("newVar"),
        oldVariableName = Some(VariableName("var3")),
        name = "newVar",
        caption = "newVar",
        datatype = Datatype.Xml,
        defaultValue = "<name>Valery</name>",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        businessProcess0 <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.storeBusinessProcessVariable(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)

      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[BusinessProcess]
          .withFieldComputed(
            _.variables,
            _.variables -
              updatePayload.oldVariableName.get.value +
              (updatePayload.variableName.value -> updatePayload.transformInto[BusinessProcessVariable])
          )
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess0 shouldBe targetSchema0
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "change variable for non-existing business process" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val updatePayload = StoreBusinessProcessVariablePayload(
        businessProcessId = id,
        variableName = VariableName("var3"),
        name = "newVar",
        caption = "newVar",
        datatype = Datatype.Xml,
        defaultValue = "<name>Valery</name>",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](
                businessProcessService.storeBusinessProcessVariable(updatePayload)
              )
      } yield ex shouldBe BusinessProcessNotFound(id.value)
    }

    "delete variable" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val createPayload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = DeleteBusinessProcessVariablePayload(
        businessProcessId = id,
        variableName = VariableName("var3"),
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        businessProcess0 <- businessProcessService.createBusinessProcess(createPayload)
        businessProcess1 <- businessProcessService.deleteBusinessProcessVariable(updatePayload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)

      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[BusinessProcess]
          .withFieldComputed(
            _.variables,
            _.variables - updatePayload.variableName.value
          )
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess0 shouldBe targetSchema0
        businessProcess1 shouldBe targetSchema1
        businessProcess2 shouldBe targetSchema1
      }
    }

    "delete variable for non-existing business process" in {
      val id            = BusinessProcessId(UUID.randomUUID().toString)
      val updatePayload = DeleteBusinessProcessVariablePayload(
        businessProcessId = id,
        variableName = VariableName("var3"),
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](
                businessProcessService.deleteBusinessProcessVariable(updatePayload)
              )
      } yield ex shouldBe BusinessProcessNotFound(id.value)
    }

    "delete business process" in {
      val id      = BusinessProcessId(UUID.randomUUID().toString)
      val payload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        businessProcess1 <- businessProcessService.createBusinessProcess(payload)
        done <- businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, payload.updatedBy))
        ex   <-
          recoverToExceptionIf[AnnetteTransportException](businessProcessService.getBusinessProcessById(id.value, true))
      } yield {
        val targetSchema = payload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess1 shouldBe targetSchema
        done shouldBe Done
        ex shouldBe BusinessProcessNotFound(id.value)
      }
    }

    "get business process by id with & w/o variables" in {
      val id      = BusinessProcessId(UUID.randomUUID().toString)
      val payload = CreateBusinessProcessPayload(
        id = id,
        name = "business process name",
        description = "business process description",
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> BusinessProcessVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> BusinessProcessVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        businessProcess1 <- businessProcessService.createBusinessProcess(payload)
        businessProcess2 <- businessProcessService.getBusinessProcessById(id.value, true)
        businessProcess3 <- businessProcessService.getBusinessProcessById(id.value, false)
      } yield {
        businessProcessService.deleteBusinessProcess(DeleteBusinessProcessPayload(id, payload.updatedBy))
        val targetSchema = payload
          .into[BusinessProcess]
          .withFieldConst(_.updatedAt, businessProcess1.updatedAt)
          .transform
        businessProcess1 shouldBe targetSchema
        businessProcess2 shouldBe targetSchema
        businessProcess3 shouldBe targetSchema.copy(variables = Map.empty)
      }
    }

  }
}
