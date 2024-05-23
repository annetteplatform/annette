package biz.lobachev.annette.bpm_repository.test

import akka.Done
import biz.lobachev.annette.bpm_repository.api.bp.{
  BusinessProcessVariable,
  CreateBusinessProcessPayload,
  DeleteBusinessProcessPayload
}
import biz.lobachev.annette.bpm_repository.api.domain.{BusinessProcessId, DataSchemaId, Datatype, VariableName}
import biz.lobachev.annette.bpm_repository.api.schema._
import biz.lobachev.annette.bpm_repository.impl.DBProvider
import biz.lobachev.annette.bpm_repository.impl.bp.{BusinessProcessActions, BusinessProcessService}
import biz.lobachev.annette.bpm_repository.impl.schema.{DataSchemaActions, DataSchemaService}
import biz.lobachev.annette.core.exception.AnnetteTransportException
import biz.lobachev.annette.core.model.auth.PersonPrincipal
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import java.util.UUID
import scala.concurrent.ExecutionContext

class DataSchemaServiceSpec extends AsyncWordSpecLike with Matchers {
  implicit val ec            = ExecutionContext.global
  val db                     = DBProvider.databaseFactory("bpm-repository-db-test")
  val actions                = new DataSchemaActions
  val dataSchemaService      = new DataSchemaService(db, actions)
  val businessProcessActions = new BusinessProcessActions
  val businessProcessService = new BusinessProcessService(db, businessProcessActions)

  "DataSchemaService" should {

    "create schema" in {
      val id      = DataSchemaId(UUID.randomUUID().toString)
      val payload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        dataSchema1 <- dataSchemaService.createDataSchema(payload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)
      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, payload.updatedBy))
        val targetSchema = payload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema1 shouldBe targetSchema
        dataSchema2 shouldBe targetSchema
      }
    }

    "update schema" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val createPayload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateDataSchemaPayload(
        id = id,
        name = "updated schema name",
        description = "updated schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hi"),
          "int2" -> DataSchemaVariable("int2", "int2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "false"),
          "json" -> DataSchemaVariable("json", "json", Datatype.Json, "{\"principalType\": \"person\"}")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        dataSchema0 <- dataSchemaService.createDataSchema(createPayload)
        dataSchema1 <- dataSchemaService.updateDataSchema(updatePayload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)

      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema0.updatedAt)
          .transform
        val targetSchema1 = updatePayload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema0 shouldBe targetSchema0
        dataSchema1 shouldBe targetSchema1
        dataSchema2 shouldBe targetSchema1
      }
    }

    "update schema name" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val createPayload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateDataSchemaNamePayload(
        id = id,
        name = "updated schema name",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        dataSchema0 <- dataSchemaService.createDataSchema(createPayload)
        dataSchema1 <- dataSchemaService.updateDataSchemaName(updatePayload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)

      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[DataSchema]
          .withFieldConst(_.name, updatePayload.name)
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema0 shouldBe targetSchema0
        dataSchema1 shouldBe targetSchema1
        dataSchema2 shouldBe targetSchema1
      }
    }

    "update schema description" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val createPayload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = UpdateDataSchemaDescriptionPayload(
        id = id,
        description = "updated schema description",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        dataSchema0 <- dataSchemaService.createDataSchema(createPayload)
        dataSchema1 <- dataSchemaService.updateDataSchemaDescription(updatePayload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)

      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[DataSchema]
          .withFieldConst(_.description, updatePayload.description)
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema0 shouldBe targetSchema0
        dataSchema1 shouldBe targetSchema1
        dataSchema2 shouldBe targetSchema1
      }
    }

    "add variable" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val createPayload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = StoreDataSchemaVariablePayload(
        dataSchemaId = id,
        variableName = VariableName("newVar"),
        name = "newVar",
        caption = "newVar",
        datatype = Datatype.Xml,
        defaultValue = "<name>Valery</name>",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        dataSchema0 <- dataSchemaService.createDataSchema(createPayload)
        dataSchema1 <- dataSchemaService.storeDataSchemaVariable(updatePayload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)

      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[DataSchema]
          .withFieldComputed(
            _.variables,
            _.variables + (updatePayload.variableName.value -> updatePayload.transformInto[DataSchemaVariable])
          )
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema0 shouldBe targetSchema0
        dataSchema1 shouldBe targetSchema1
        dataSchema2 shouldBe targetSchema1
      }
    }

    "change variable" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val createPayload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = StoreDataSchemaVariablePayload(
        dataSchemaId = id,
        variableName = VariableName("var3"),
        name = "newVar",
        caption = "newVar",
        datatype = Datatype.Xml,
        defaultValue = "<name>Valery</name>",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        dataSchema0 <- dataSchemaService.createDataSchema(createPayload)
        dataSchema1 <- dataSchemaService.storeDataSchemaVariable(updatePayload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)

      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[DataSchema]
          .withFieldComputed(
            _.variables,
            _.variables + (updatePayload.variableName.value -> updatePayload.transformInto[DataSchemaVariable])
          )
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema0 shouldBe targetSchema0
        dataSchema1 shouldBe targetSchema1
        dataSchema2 shouldBe targetSchema1
      }
    }

    "change variable name" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val createPayload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = StoreDataSchemaVariablePayload(
        dataSchemaId = id,
        variableName = VariableName("newVar"),
        oldVariableName = Some(VariableName("var3")),
        name = "newVar",
        caption = "newVar",
        datatype = Datatype.Xml,
        defaultValue = "<name>Valery</name>",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        dataSchema0 <- dataSchemaService.createDataSchema(createPayload)
        dataSchema1 <- dataSchemaService.storeDataSchemaVariable(updatePayload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)

      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[DataSchema]
          .withFieldComputed(
            _.variables,
            _.variables -
              updatePayload.oldVariableName.get.value +
              (updatePayload.variableName.value -> updatePayload.transformInto[DataSchemaVariable])
          )
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema0 shouldBe targetSchema0
        dataSchema1 shouldBe targetSchema1
        dataSchema2 shouldBe targetSchema1
      }
    }

    "change variable for non-existing schema" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val updatePayload = StoreDataSchemaVariablePayload(
        dataSchemaId = id,
        variableName = VariableName("var3"),
        name = "newVar",
        caption = "newVar",
        datatype = Datatype.Xml,
        defaultValue = "<name>Valery</name>",
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](dataSchemaService.storeDataSchemaVariable(updatePayload))
      } yield ex shouldBe DataSchemaNotFound(id.value)
    }

    "delete variable" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val createPayload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val updatePayload = DeleteDataSchemaVariablePayload(
        dataSchemaId = id,
        variableName = VariableName("var3"),
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        dataSchema0 <- dataSchemaService.createDataSchema(createPayload)
        dataSchema1 <- dataSchemaService.deleteDataSchemaVariable(updatePayload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)

      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, createPayload.updatedBy))
        val targetSchema0 = createPayload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema0.updatedAt)
          .transform
        val targetSchema1 = createPayload
          .into[DataSchema]
          .withFieldComputed(
            _.variables,
            _.variables - updatePayload.variableName.value
          )
          .withFieldConst(_.updatedBy, updatePayload.updatedBy)
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema0 shouldBe targetSchema0
        dataSchema1 shouldBe targetSchema1
        dataSchema2 shouldBe targetSchema1
      }
    }

    "delete variable for non-existing schema" in {
      val id            = DataSchemaId(UUID.randomUUID().toString)
      val updatePayload = DeleteDataSchemaVariablePayload(
        dataSchemaId = id,
        variableName = VariableName("var3"),
        updatedBy = PersonPrincipal("P0002")
      )
      for {
        ex <- recoverToExceptionIf[AnnetteTransportException](dataSchemaService.deleteDataSchemaVariable(updatePayload))
      } yield ex shouldBe DataSchemaNotFound(id.value)
    }

    "delete schema" in {
      val id      = DataSchemaId(UUID.randomUUID().toString)
      val payload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        dataSchema1 <- dataSchemaService.createDataSchema(payload)
        done <- dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, payload.updatedBy))
        ex   <- recoverToExceptionIf[AnnetteTransportException](dataSchemaService.getDataSchema(id.value, true))
      } yield {
        val targetSchema = payload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema1 shouldBe targetSchema
        done shouldBe Done
        ex shouldBe DataSchemaNotFound(id.value)
      }
    }

    "delete schema with reference" in {
      val id                     = DataSchemaId(UUID.randomUUID().toString)
      val payload                = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      val businessProcessId      = BusinessProcessId(UUID.randomUUID().toString)
      val businessProcessPayload = CreateBusinessProcessPayload(
        id = businessProcessId,
        name = "business process name",
        description = "business process description",
        dataSchemaId = Some(id),
        variables = Map(
          "var1" -> BusinessProcessVariable("var1", "var1", Datatype.String, "hello"),
          "var4" -> BusinessProcessVariable("var4", "var4", Datatype.Integer, "123"),
          "var5" -> BusinessProcessVariable("var5", "var5", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        _ <- dataSchemaService.createDataSchema(payload)
        _  <- businessProcessService.createBusinessProcess(businessProcessPayload)
        ex <- recoverToExceptionIf[AnnetteTransportException](
                dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, payload.updatedBy))
              )
      } yield {
        businessProcessService
          .deleteBusinessProcess(DeleteBusinessProcessPayload(businessProcessId, PersonPrincipal("P0001")))
          .andThen(_ => dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, payload.updatedBy)))
        ex shouldBe DataSchemaHasReference(id.value)
      }
    }

    "get schema by id with & w/o variables" in {
      val id      = DataSchemaId(UUID.randomUUID().toString)
      val payload = CreateDataSchemaPayload(
        id = id,
        name = "schema name",
        description = "schema description",
        variables = Map(
          "var1" -> DataSchemaVariable("var1", "var1", Datatype.String, "hello"),
          "var2" -> DataSchemaVariable("var2", "var2", Datatype.Integer, "123"),
          "var3" -> DataSchemaVariable("var3", "var3", Datatype.Boolean, "true")
        ),
        updatedBy = PersonPrincipal("P0001")
      )
      for {
        dataSchema1 <- dataSchemaService.createDataSchema(payload)
        dataSchema2 <- dataSchemaService.getDataSchema(id.value, true)
        dataSchema3 <- dataSchemaService.getDataSchema(id.value, false)
      } yield {
        dataSchemaService.deleteDataSchema(DeleteDataSchemaPayload(id, payload.updatedBy))
        val targetSchema = payload
          .into[DataSchema]
          .withFieldConst(_.updatedAt, dataSchema1.updatedAt)
          .transform
        dataSchema1 shouldBe targetSchema
        dataSchema2 shouldBe targetSchema
        dataSchema3 shouldBe targetSchema.copy(variables = Map.empty)
      }
    }

  }
}
