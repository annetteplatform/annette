package biz.lobachev.annette.cms.impl.space.model

import biz.lobachev.annette.cms.impl.space.SpaceEntity
import biz.lobachev.annette.cms.api.space.{Space, SpaceAnnotation}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object SpaceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[SpaceState],
      JsonSerializer[SpaceAnnotation],
      JsonSerializer[Space],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[SpaceEntity.Success.type],
      JsonSerializer[SpaceEntity.SuccessSpace],
      JsonSerializer[SpaceEntity.SuccessSpaceAnnotation],
      JsonSerializer[SpaceEntity.SpaceAlreadyExist.type],
      JsonSerializer[SpaceEntity.SpaceNotFound.type],
      JsonSerializer[SpaceEntity.SpaceCreated],
      JsonSerializer[SpaceEntity.SpaceNameUpdated],
      JsonSerializer[SpaceEntity.SpaceDescriptionUpdated],
      JsonSerializer[SpaceEntity.SpaceCategoryUpdated],
      JsonSerializer[SpaceEntity.SpaceTargetPrincipalAssigned],
      JsonSerializer[SpaceEntity.SpaceTargetPrincipalUnassigned],
      JsonSerializer[SpaceEntity.SpaceActivated],
      JsonSerializer[SpaceEntity.SpaceDeactivated],
      JsonSerializer[SpaceEntity.SpaceDeleted]
    )
}
