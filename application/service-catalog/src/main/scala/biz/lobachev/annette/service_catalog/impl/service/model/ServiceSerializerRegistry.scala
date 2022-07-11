package biz.lobachev.annette.service_catalog.impl.service.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.service.{Service, ServiceId, ServiceLink}
import biz.lobachev.annette.service_catalog.impl.service.ServiceEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object ServiceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[ServiceState],
      JsonSerializer[Service],
      JsonSerializer[ServiceLink],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[ServiceId],
      JsonSerializer[MultiLanguageText],
      JsonSerializer[ServiceEntity.Success.type],
      JsonSerializer[ServiceEntity.SuccessService],
      JsonSerializer[ServiceEntity.AlreadyExist.type],
      JsonSerializer[ServiceEntity.NotFound.type],
      JsonSerializer[ServiceEntity.ServiceCreated],
      JsonSerializer[ServiceEntity.ServiceUpdated],
      JsonSerializer[ServiceEntity.ServiceActivated],
      JsonSerializer[ServiceEntity.ServiceDeactivated],
      JsonSerializer[ServiceEntity.ServiceDeleted]
    )
}
