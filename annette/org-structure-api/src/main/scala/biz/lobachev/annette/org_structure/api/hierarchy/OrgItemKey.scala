package biz.lobachev.annette.org_structure.api.hierarchy

import play.api.libs.json.Json

case class OrgItemKey(
  orgId: String,
  itemId: Option[String]
) {
  def toComposed: CompositeOrgItemId = itemId.map(subId => s"$orgId${OrgItemKey.SEPARATOR}$subId").getOrElse(orgId)
}

object OrgItemKey {
  final val SEPARATOR = "/"

  def fromComposed(id: CompositeOrgItemId): OrgItemKey = {
    val splited = id.split(SEPARATOR)
    if (splited.length < 1 || splited.length > 2) throw InvalidCompositeId(id)
    try {
      val orgId = splited(0)
      val subId =
        if (splited.length == 2)
          Some(splited(1))
        else
          None
      OrgItemKey(orgId, subId)
    } catch {
      case _: Throwable => throw InvalidCompositeId(id)
    }
  }

  def extractOrgId(id: CompositeOrgItemId): String = fromComposed(id).orgId

  def isOrg(id: CompositeOrgItemId): Boolean = fromComposed(id).itemId.isEmpty

  implicit val format = Json.format[OrgItemKey]
}
