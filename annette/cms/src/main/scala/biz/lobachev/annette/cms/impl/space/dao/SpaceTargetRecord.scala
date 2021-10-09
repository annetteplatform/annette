package biz.lobachev.annette.cms.impl.space.dao

import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal

case class SpaceTargetRecord(
  spaceId: SpaceId,
  principal: AnnettePrincipal
) {}
