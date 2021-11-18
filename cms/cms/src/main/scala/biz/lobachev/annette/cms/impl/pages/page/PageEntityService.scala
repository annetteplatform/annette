/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.cms.impl.pages.page

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.impl.pages.page.dao.{PageDbDao, PageIndexDao}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import scala.collection.immutable.Set
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class PageEntityService(
  clusterSharding: ClusterSharding,
  dbDao: PageDbDao,
  indexDao: PageIndexDao
)(implicit
  ec: ExecutionContext
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: PageId): EntityRef[PageEntity.Command] =
    clusterSharding.entityRefFor(PageEntity.typeKey, id)

  private def convertSuccess(confirmation: PageEntity.Confirmation, id: PageId, maybeId: Option[String] = None): Done =
    confirmation match {
      case PageEntity.Success                            => Done
      case PageEntity.PageAlreadyExist                   => throw PageAlreadyExist(id)
      case PageEntity.PageNotFound                       => throw PageNotFound(id)
      case PageEntity.PagePublicationDateClearNotAllowed => throw PagePublicationDateClearNotAllowed(id)
      case PageEntity.WidgetContentNotFound              => throw WidgetContentNotFound(id, maybeId.getOrElse(""))
      case _                                             => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPage(confirmation: PageEntity.Confirmation, id: PageId): Page =
    confirmation match {
      case PageEntity.SuccessPage(page) => page
      case PageEntity.PageAlreadyExist  => throw PageAlreadyExist(id)
      case PageEntity.PageNotFound      => throw PageNotFound(id)
      case _                            => throw new RuntimeException("Match fail")
    }

  def createPage(payload: CreatePagePayload, targets: Set[AnnettePrincipal]): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.CreatePage]
          .withFieldConst(_.targets, targets)
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePageFeatured(payload: UpdatePageFeaturedPayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdatePageFeatured]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePageAuthor(payload: UpdatePageAuthorPayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdatePageAuthor]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePageTitle(payload: UpdatePageTitlePayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdatePageTitle]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateWidgetContent(payload: UpdatePageWidgetContentPayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdateWidgetContent]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widgetContent.id)))

  def changeWidgetContentOrder(payload: ChangePageWidgetContentOrderPayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.ChangeWidgetContentOrder]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widgetContentId)))

  def deleteWidgetContent(payload: DeletePageWidgetContentPayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.DeleteWidgetContent]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widgetContentId)))

  def updatePagePublicationTimestamp(payload: UpdatePagePublicationTimestampPayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdatePagePublicationTimestamp]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def publishPage(payload: PublishPagePayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.PublishPage]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unpublishPage(payload: UnpublishPagePayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UnpublishPage]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignPageTargetPrincipal(payload: AssignPageTargetPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.AssignPageTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignPageTargetPrincipal(payload: UnassignPageTargetPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UnassignPageTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deletePage(payload: DeletePagePayload): Future[Done] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.DeletePage]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def getPage(id: PageId, withIntro: Boolean, withContent: Boolean, withTargets: Boolean): Future[Page] =
    refFor(id)
      .ask[PageEntity.Confirmation](
        PageEntity.GetPage(id, withIntro, withContent, withTargets, _)
      )
      .map(convertSuccessPage(_, id))

  def canAccessToPage(payload: CanAccessToPagePayload): Future[Boolean] =
    dbDao.canAccessToPage(payload.id, payload.principals)

  def getPageById(
    id: PageId,
    fromReadSide: Boolean,
    withIntro: Boolean,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Page] =
    if (fromReadSide)
      dbDao
        .getPageById(id, withIntro, withContent, withTargets)
        .map(_.getOrElse(throw PageNotFound(id)))
    else
      getPage(id, withIntro, withContent, withTargets)

  def getPagesById(
    ids: Set[PageId],
    fromReadSide: Boolean,
    withIntro: Boolean,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Seq[Page]] =
    if (fromReadSide)
      dbDao.getPagesById(ids, withIntro, withContent, withTargets)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[PageEntity.Confirmation](PageEntity.GetPage(id, withIntro, withContent, withTargets, _))
            .map {
              case PageEntity.SuccessPage(page) => Some(page)
              case _                            => None
            }
        }
        .map(_.flatten.toSeq)

  def getPageViews(payload: GetPageViewsPayload): Future[Seq[PageView]] =
    dbDao.getPageViewsById(payload)

  def findPages(query: PageFindQuery): Future[FindResult] = indexDao.findPages(query)

  def viewPage(payload: ViewPagePayload): Future[Done] = dbDao.viewPage(payload.id, payload.updatedBy)

  def likePage(payload: LikePagePayload): Future[Done] = dbDao.likePage(payload.id, payload.updatedBy)

  def unlikePage(payload: UnlikePagePayload): Future[Done] = dbDao.unlikePage(payload.id, payload.updatedBy)

  def getPageMetricById(payload: GetPageMetricPayload): Future[PageMetric] =
    dbDao.getPageMetricById(payload.id, payload.principal)

  def getPageMetricsById(payload: GetPageMetricsPayload): Future[Seq[PageMetric]] =
    dbDao.getPageMetricsById(payload.ids, payload.principal)

}
