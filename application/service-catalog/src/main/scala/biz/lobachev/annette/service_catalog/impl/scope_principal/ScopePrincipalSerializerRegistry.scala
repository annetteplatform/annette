package biz.lobachev.annette.service_catalog.impl.scope_principal

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.scope_principal._

object ScopePrincipalSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[ScopePrincipalState],
    JsonSerializer[ScopePrincipal],
    JsonSerializer[AnnettePrincipal],
    JsonSerializer[ScopeId],
    JsonSerializer[ScopePrincipalEntity.Success.type],
    JsonSerializer[ScopePrincipalEntity.ScopePrincipalNotFound.type],
    JsonSerializer[ScopePrincipalEntity.ScopePrincipalAssigned],
    JsonSerializer[ScopePrincipalEntity.ScopePrincipalUnassigned],
  )
}
