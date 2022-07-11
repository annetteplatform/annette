package biz.lobachev.annette.service_catalog.impl.group.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.group.{Group, GroupId}
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import biz.lobachev.annette.service_catalog.impl.group.GroupEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object GroupSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[GroupState],
      JsonSerializer[Group],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[GroupId],
      JsonSerializer[ServiceId],
      JsonSerializer[MultiLanguageText],
      JsonSerializer[GroupEntity.Success.type],
      JsonSerializer[GroupEntity.SuccessGroup],
      JsonSerializer[GroupEntity.AlreadyExist.type],
      JsonSerializer[GroupEntity.NotFound.type],
      JsonSerializer[GroupEntity.GroupCreated],
      JsonSerializer[GroupEntity.GroupUpdated],
      JsonSerializer[GroupEntity.GroupActivated],
      JsonSerializer[GroupEntity.GroupDeactivated],
      JsonSerializer[GroupEntity.GroupDeleted]
    )
}
