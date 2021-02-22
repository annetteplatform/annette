package biz.lobachev.annette.ignition.core

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.model.ServiceLoadResult

import scala.concurrent.Future

trait ServiceLoader {
  def run(principal: AnnettePrincipal): Future[ServiceLoadResult]
}
