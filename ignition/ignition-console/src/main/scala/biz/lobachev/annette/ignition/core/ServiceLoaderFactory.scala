package biz.lobachev.annette.ignition.core

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.typesafe.config.Config

trait ServiceLoaderFactory {

  def create(client: IgnitionLagomClient, config: Config, principal: AnnettePrincipal): ServiceLoader

}
