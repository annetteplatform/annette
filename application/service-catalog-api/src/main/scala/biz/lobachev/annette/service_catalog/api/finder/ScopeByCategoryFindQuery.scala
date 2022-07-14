package biz.lobachev.annette.service_catalog.api.finder

import biz.lobachev.annette.core.model.category.CategoryId
import play.api.libs.json.{Format, Json}

case class ScopeByCategoryFindQuery(
  categories: Set[CategoryId],
  principalCodes: Set[String]
)

object ScopeByCategoryFindQuery {
  implicit val format: Format[ScopeByCategoryFindQuery] = Json.format
}
