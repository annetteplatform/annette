package biz.lobachev.annette.service_catalog.impl.service

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.service._

object ServiceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[ServiceState],
    JsonSerializer[Service],
    JsonSerializer[OffsetDateTime],
    JsonSerializer[AnnettePrincipal],
    JsonSerializer[ServiceId],
    JsonSerializer[CategoryId],
    JsonSerializer[ServiceLink],
    JsonSerializer[Caption],
    JsonSerializer[ServiceEntity.Success.type],
    JsonSerializer[ServiceEntity.SuccessService],
    JsonSerializer[ServiceEntity.ServiceAlreadyExist.type],
    JsonSerializer[ServiceEntity.ServiceNotFound.type],
    JsonSerializer[ServiceEntity.ServiceCreated],
    JsonSerializer[ServiceEntity.ServiceUpdated],
    JsonSerializer[ServiceEntity.ServiceActivated],
    JsonSerializer[ServiceEntity.ServiceDeactivated],
    JsonSerializer[ServiceEntity.ServiceDeleted],
  )
}
