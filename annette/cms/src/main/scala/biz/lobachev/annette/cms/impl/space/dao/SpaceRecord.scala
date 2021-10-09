package biz.lobachev.annette.cms.impl.space.dao

import biz.lobachev.annette.cms.api.space.{Space, SpaceId, SpaceView}
import biz.lobachev.annette.cms.api.space.SpaceType.SpaceType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId

import java.time.OffsetDateTime

case class SpaceRecord(
  id: SpaceId,
  name: String,
  description: String,
  spaceType: SpaceType,
  categoryId: CategoryId,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toSpaceView = SpaceView(id, name, description, spaceType, categoryId, active, updatedBy, updatedAt)

  def toSpace = Space(id, name, description, spaceType, categoryId, Set.empty, active, updatedBy, updatedAt)
}
