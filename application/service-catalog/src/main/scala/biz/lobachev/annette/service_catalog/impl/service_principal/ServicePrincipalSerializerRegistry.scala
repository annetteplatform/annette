package biz.lobachev.annette.service_catalog.impl.service_principal

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.service_principal._

object ServicePrincipalSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[ServicePrincipalState],
    JsonSerializer[ServicePrincipal],
    JsonSerializer[AnnettePrincipal],
    JsonSerializer[ScopeId],
    JsonSerializer[ServicePrincipalEntity.Success.type],
    JsonSerializer[ServicePrincipalEntity.ServicePrincipalNotFound.type],
    JsonSerializer[ServicePrincipalEntity.ServicePrincipalAssigned],
    JsonSerializer[ServicePrincipalEntity.ServicePrincipalUnassigned],
  )
}
