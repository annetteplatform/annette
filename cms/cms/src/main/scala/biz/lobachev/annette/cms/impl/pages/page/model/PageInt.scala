package biz.lobachev.annette.cms.impl.pages.page.model

import biz.lobachev.annette.cms.api.common.article.{Metric, PublicationStatus}
import biz.lobachev.annette.cms.api.pages.page.{Page, PageId}
import biz.lobachev.annette.cms.api.pages.space.SpaceId
import biz.lobachev.annette.cms.impl.content.ContentInt
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}
import io.scalaland.chimney.dsl._

import java.time.OffsetDateTime

case class PageInt(
  id: PageId,
  spaceId: SpaceId,
  authorId: AnnettePrincipal,
  title: String,
  publicationStatus: PublicationStatus.PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  content: Option[ContentInt] = None,
  targets: Option[Set[AnnettePrincipal]] = None,
  metric: Option[Metric] = None,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toPage: Page =
    this
      .into[Page]
      .withFieldComputed(_.content, _.content.map(_.toContent))
      .transform

}

object PageInt {
  implicit val format: Format[PageInt] = Json.format
}
