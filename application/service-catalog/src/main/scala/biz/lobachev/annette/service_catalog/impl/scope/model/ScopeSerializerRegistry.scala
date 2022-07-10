package biz.lobachev.annette.service_catalog.impl.scope.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.group.GroupId
import biz.lobachev.annette.service_catalog.api.scope.{Scope, ScopeId}
import biz.lobachev.annette.service_catalog.impl.scope.ScopeEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object ScopeSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[ScopeState],
      JsonSerializer[Scope],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[ScopeId],
      JsonSerializer[GroupId],
      JsonSerializer[ScopeEntity.Success.type],
      JsonSerializer[ScopeEntity.SuccessScope],
      JsonSerializer[ScopeEntity.AlreadyExist.type],
      JsonSerializer[ScopeEntity.NotFound.type],
      JsonSerializer[ScopeEntity.ScopeCreated],
      JsonSerializer[ScopeEntity.ScopeUpdated],
      JsonSerializer[ScopeEntity.ScopeActivated],
      JsonSerializer[ScopeEntity.ScopeDeactivated],
      JsonSerializer[ScopeEntity.ScopeDeleted]
    )
}
