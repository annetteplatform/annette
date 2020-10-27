package biz.lobachev.annette.attributes.impl.schema.model

import java.time.OffsetDateTime

import biz.lobachev.annette.attributes.api.attribute.{Attribute, AttributeIndex, AttributeType}
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.attributes.impl.schema.SchemaEntity
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object SchemaSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[AttributeIndexState],
      JsonSerializer[AttributeState],
      JsonSerializer[SchemaState],
      JsonSerializer[Attribute],
      JsonSerializer[AttributeIndex],
      JsonSerializer[AttributeType],
      JsonSerializer[Schema],
      JsonSerializer[SchemaId],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[DeleteSchemaPayload],
      JsonSerializer[ActivateSchemaPayload],
      JsonSerializer[UpdateSchemaPayload],
      JsonSerializer[CreateSchemaPayload],
      JsonSerializer[SchemaEntity.Success.type],
      JsonSerializer[SchemaEntity.SuccessSchema],
      JsonSerializer[SchemaEntity.SuccessSchemaAttribute],
      JsonSerializer[SchemaEntity.SuccessSchemaAttributes],
      JsonSerializer[SchemaEntity.SchemaAlreadyExist.type],
      JsonSerializer[SchemaEntity.SchemaNotFound.type],
      JsonSerializer[SchemaEntity.EmptySchema.type],
      JsonSerializer[SchemaEntity.TypeChangeNotAllowed.type],
      JsonSerializer[SchemaEntity.AttributeNotFound.type],
      JsonSerializer[SchemaEntity.AttributesHasAssignments],
      JsonSerializer[SchemaEntity.SchemaCreated],
      JsonSerializer[SchemaEntity.SchemaNameUpdated],
      JsonSerializer[SchemaEntity.ActiveAttributeCreated],
      JsonSerializer[SchemaEntity.ActiveAttributeUpdated],
      JsonSerializer[SchemaEntity.ActiveAttributeRemoved],
      JsonSerializer[SchemaEntity.IndexAttributeCreated],
      JsonSerializer[SchemaEntity.IndexAttributeRemoved],
      JsonSerializer[SchemaEntity.PreparedAttributeCreated],
      JsonSerializer[SchemaEntity.PreparedAttributeUpdated],
      JsonSerializer[SchemaEntity.PreparedAttributeRemoved],
      JsonSerializer[SchemaEntity.SchemaDeleted]
    )
}
