package biz.lobachev.annette.ignition.authorization.loaders.data

import biz.lobachev.annette.core.model.PermissionId
import biz.lobachev.annette.core.model.auth.Permission
import play.api.libs.json.Json

case class PermissionData(
  id: PermissionId,
  arg1: Option[String] = None,
  arg2: Option[String] = None,
  arg3: Option[String] = None
) {
  def toPermission: Permission =
    Permission(
      id,
      arg1.getOrElse(""),
      arg2.getOrElse(""),
      arg3.getOrElse("")
    )
}

object PermissionData {
  implicit val format = Json.format[PermissionData]
}
