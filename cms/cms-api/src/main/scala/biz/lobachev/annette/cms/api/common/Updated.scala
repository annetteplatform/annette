package biz.lobachev.annette.cms.api.common

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class Updated(
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime
)

object Updated {
  implicit val format: Format[Updated] = Json.format
}
