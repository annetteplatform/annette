package biz.lobachev.annette.core.model.indexing

import biz.lobachev.annette.core.utils.Encase
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

sealed trait AdvancedQuery

case class Exist(alias: String) extends AdvancedQuery
object Exist {
  implicit val format: Format[Exist] = Json.format
}

case class NotExist(alias: String) extends AdvancedQuery
object NotExist {
  implicit val format: Format[NotExist] = Json.format
}

case class Equal(alias: String, value: String) extends AdvancedQuery
object Equal {
  implicit val format: Format[Equal] = Json.format
}

case class NotEqual(alias: String, value: String) extends AdvancedQuery
object NotEqual {
  implicit val format: Format[NotEqual] = Json.format
}

case class AnyOf(alias: String, values: Set[String]) extends AdvancedQuery
object AnyOf {
  implicit val format: Format[AnyOf] = Json.format
}

case class Range(
  alias: String,
  gt: Option[String] = None,
  gte: Option[String] = None,
  lt: Option[String] = None,
  lte: Option[String] = None
) extends AdvancedQuery
object Range {
  implicit val format: Format[Range] = Json.format
}

object AdvancedQuery {
  implicit val config                        = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      Encase.toLowerKebab(fullName.split("\\.").toSeq.last)
    }
  )
  implicit val format: Format[AdvancedQuery] = Json.format
}
