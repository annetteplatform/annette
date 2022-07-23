package biz.lobachev.annette.ignition.org_structure.loaders.data

import biz.lobachev.annette.core.attribute.AttributeValues
import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

sealed trait OrgItemData {
  val id: String
  val name: String
  val categoryId: OrgCategoryId
  val source: Option[String]
  val externalId: Option[String]
  val attributes: Option[AttributeValues]
}

case class PositionData(
  id: String,
  name: String,
  limit: Int = 1,
  categoryId: OrgCategoryId,
  persons: Option[Set[String]] = None,
  source: Option[String] = None,
  externalId: Option[String] = None,
  attributes: Option[AttributeValues] = None
) extends OrgItemData

object PositionData {
  implicit val format = Json.format[PositionData]
}

case class UnitData(
  id: String,
  name: String,
  chief: Option[String] = None,
  children: Seq[OrgItemData] = Seq.empty,
  categoryId: OrgCategoryId,
  source: Option[String] = None,
  externalId: Option[String] = None,
  attributes: Option[AttributeValues] = None
) extends OrgItemData

object UnitData {
  implicit val format = Json.format[UnitData]
}

object OrgItemData {
  implicit val config                      = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "PositionData" => "P"
        case "UnitData"     => "U"
      }
    }
  )
  implicit val format: Format[OrgItemData] = Json.format
}
