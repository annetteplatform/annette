package biz.lobachev.annette.service_catalog.impl.group.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.group.{Group, GroupId}
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import io.scalaland.chimney.dsl.TransformerOps
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class GroupState(
  id: GroupId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  services: Seq[ServiceId] = Seq.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toGroup(): Group = this.transformInto[Group]

}

object GroupState {
  implicit val format: Format[GroupState] = Json.format
}
