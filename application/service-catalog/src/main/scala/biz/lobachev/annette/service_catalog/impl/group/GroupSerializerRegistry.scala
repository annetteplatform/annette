package biz.lobachev.annette.service_catalog.impl.group

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.group._

object GroupSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[GroupState],
    JsonSerializer[Group],
    JsonSerializer[OffsetDateTime],
    JsonSerializer[AnnettePrincipal],
    JsonSerializer[GroupId],
    JsonSerializer[CategoryId],
    JsonSerializer[ServiceId],
    JsonSerializer[Caption],
    JsonSerializer[GroupEntity.Success.type],
    JsonSerializer[GroupEntity.SuccessGroup],
    JsonSerializer[GroupEntity.GroupAlreadyExist.type],
    JsonSerializer[GroupEntity.GroupNotFound.type],
    JsonSerializer[GroupEntity.GroupCreated],
    JsonSerializer[GroupEntity.GroupUpdated],
    JsonSerializer[GroupEntity.GroupActivated],
    JsonSerializer[GroupEntity.GroupDeactivated],
    JsonSerializer[GroupEntity.GroupDeleted],
  )
}
