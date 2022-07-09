package biz.lobachev.annette.service_catalog.impl.scope

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.scope._

object ScopeSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[ScopeState],
    JsonSerializer[Scope],
    JsonSerializer[OffsetDateTime],
    JsonSerializer[AnnettePrincipal],
    JsonSerializer[ScopeId],
    JsonSerializer[GroupId],
    JsonSerializer[CategoryId],
    JsonSerializer[ScopeEntity.Success.type],
    JsonSerializer[ScopeEntity.SuccessScope],
    JsonSerializer[ScopeEntity.ScopeAlreadyExist.type],
    JsonSerializer[ScopeEntity.ScopeNotFound.type],
    JsonSerializer[ScopeEntity.ScopeCreated],
    JsonSerializer[ScopeEntity.ScopeUpdated],
    JsonSerializer[ScopeEntity.ScopeActivated],
    JsonSerializer[ScopeEntity.ScopeDeactivated],
    JsonSerializer[ScopeEntity.ScopeDeleted],
  )
}
