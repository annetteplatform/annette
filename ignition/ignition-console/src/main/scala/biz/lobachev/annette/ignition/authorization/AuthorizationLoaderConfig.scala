package biz.lobachev.annette.ignition.authorization

import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, ErrorMode, ServiceLoaderConfig}
import com.typesafe.config.Config

import scala.util.Try

case class AuthorizationLoaderConfig(
  entities: Seq[String],
  onError: ErrorMode,
  role: Option[DefaultEntityLoaderConfig],
  roleAssignment: Option[DefaultEntityLoaderConfig]
) extends ServiceLoaderConfig {}

object AuthorizationLoaderConfig {
  def apply(config: Config): AuthorizationLoaderConfig =
    AuthorizationLoaderConfig(
      entities = ServiceLoaderConfig.entities(config),
      onError = ErrorMode.fromConfig(config),
      role = Try(DefaultEntityLoaderConfig(config.getConfig(AuthorizationLoader.Role))).toOption,
      roleAssignment = Try(DefaultEntityLoaderConfig(config.getConfig(AuthorizationLoader.RoleAssignment))).toOption
    )
}
