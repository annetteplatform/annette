package biz.lobachev.annette.service_catalog.impl.service.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.Caption
import biz.lobachev.annette.service_catalog.api.service.{Service, ServiceId}
import io.scalaland.chimney.dsl.TransformerOps
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class ServiceState(
  id: ServiceId,
  name: String,
  description: String,
  icon: String,
  caption: Caption,
  captionDescription: Caption,
  link: String,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toService(): Service = this.transformInto[Service]
}

object ServiceState {
  implicit val format: Format[ServiceState] = Json.format
}
