package biz.lobachev.annette.cms.api.home_page

import biz.lobachev.annette.core.model.indexing.SortBy
import play.api.libs.json.{Format, Json}

case class HomePageFindQuery(
  offset: Int = 0,
  size: Int,
  applicationId: Option[String] = None,
  principalCodes: Option[Set[String]] = None,
  principalType: Option[String] = None,
  principalId: Option[String] = None,
  pageId: Option[String] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object HomePageFindQuery {
  implicit val format: Format[HomePageFindQuery] = Json.format
}
