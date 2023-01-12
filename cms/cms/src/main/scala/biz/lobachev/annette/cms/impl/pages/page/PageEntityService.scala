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
import biz.lobachev.annette.cms.api.common.article.{
  GetMetricPayload,
  GetMetricsPayload,
  LikePayload,
  Metric,
  PublishPayload,
  UnlikePayload,
  UnpublishPayload,
  UpdateAuthorPayload,
  UpdatePublicationTimestampPayload,
  UpdateTitlePayload,
  ViewPayload
}
import biz.lobachev.annette.cms.api.common.{
  AssignPrincipalPayload,
  CanAccessToEntityPayload,
  DeletePayload,
  UnassignPrincipalPayload,
  Updated
}
import biz.lobachev.annette.cms.api.content.{
  ChangeWidgetOrderPayload,
  DeleteWidgetPayload,
  UpdateContentSettingsPayload,
  UpdateWidgetPayload
}
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.impl.content.{ContentInt, WidgetInt}
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

  private def convertSuccess(
    confirmation: PageEntity.Confirmation,
    id: PageId,
    maybeId: Option[String] = None
  ): Updated =
    confirmation match {
      case PageEntity.Success(updatedBy, updatedAt)      => Updated(updatedBy, updatedAt)
      case PageEntity.PageAlreadyExist                   => throw PageAlreadyExist(id)
      case PageEntity.PageNotFound                       => throw PageNotFound(id)
      case PageEntity.PagePublicationDateClearNotAllowed => throw PagePublicationDateClearNotAllowed(id)
      case PageEntity.WidgetNotFound                     => throw WidgetNotFound(id, maybeId.getOrElse(""))
      case _                                             => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPage(confirmation: PageEntity.Confirmation, id: PageId): Page =
    confirmation match {
      case PageEntity.SuccessPage(pageInt) => pageInt.toPage
      case PageEntity.PageAlreadyExist     => throw PageAlreadyExist(id)
      case PageEntity.PageNotFound         => throw PageNotFound(id)
      case _                               => throw new RuntimeException("Match fail")
    }

  def createPage(payload: CreatePagePayload, targets: Set[AnnettePrincipal]): Future[Page] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.CreatePage]
          .withFieldComputed(_.content, c => ContentInt.fromContent(c.content))
          .withFieldConst(_.targets, targets)
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccessPage(_, payload.id))

  def updatePageAuthor(payload: UpdateAuthorPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdatePageAuthor]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePageTitle(payload: UpdateTitlePayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdatePageTitle]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePageContentSettings(payload: UpdateContentSettingsPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdateContentSettings]
          .withFieldComputed(_.settings, _.settings.toString())
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, None))

  def updateWidget(payload: UpdateWidgetPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdateWidget]
          .withFieldComputed(_.widget, c => WidgetInt.fromWidget(c.widget))
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widget.id)))

  def changeWidgetOrder(payload: ChangeWidgetOrderPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.ChangeWidgetOrder]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widgetId)))

  def deleteWidget(payload: DeleteWidgetPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.DeleteWidget]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widgetId)))

  def updatePagePublicationTimestamp(payload: UpdatePublicationTimestampPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UpdatePagePublicationTimestamp]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def publishPage(payload: PublishPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.PublishPage]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unpublishPage(payload: UnpublishPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UnpublishPage]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignPageTargetPrincipal(payload: AssignPrincipalPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.AssignPageTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignPageTargetPrincipal(payload: UnassignPrincipalPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.UnassignPageTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deletePage(payload: DeletePayload): Future[Updated] =
    refFor(payload.id)
      .ask[PageEntity.Confirmation](replyTo =>
        payload
          .into[PageEntity.DeletePage]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def getPage(id: PageId, withContent: Boolean, withTargets: Boolean): Future[Page] =
    refFor(id)
      .ask[PageEntity.Confirmation](
        PageEntity.GetPage(id, withContent, withTargets, _)
      )
      .map(convertSuccessPage(_, id))

  def canAccessToPage(payload: CanAccessToEntityPayload): Future[Boolean] =
    dbDao.canAccessToPage(payload.id, payload.principals)

  def getPage(
    id: PageId,
    fromReadSide: Boolean,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Page] =
    if (fromReadSide)
      dbDao
        .getPage(id, withContent, withTargets)
        .map(_.getOrElse(throw PageNotFound(id)))
    else
      getPage(id, withContent, withTargets)

  def getPages(
    ids: Set[PageId],
    fromReadSide: Boolean,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Seq[Page]] =
    if (fromReadSide)
      dbDao.getPages(ids, withContent, withTargets)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[PageEntity.Confirmation](PageEntity.GetPage(id, withContent, withTargets, _))
            .map {
              case PageEntity.SuccessPage(pageInt) => Some(pageInt.toPage)
              case _                               => None
            }
        }
        .map(_.flatten.toSeq)

  def getPageViews(payload: GetPageViewsPayload): Future[Seq[Page]] =
    dbDao.getPageViews(payload)

  def findPages(query: PageFindQuery): Future[FindResult] = indexDao.findPages(query)

  def viewPage(payload: ViewPayload): Future[Done] = dbDao.viewPage(payload.id, payload.updatedBy)

  def likePage(payload: LikePayload): Future[Done] = dbDao.likePage(payload.id, payload.updatedBy)

  def unlikePage(payload: UnlikePayload): Future[Done] = dbDao.unlikePage(payload.id, payload.updatedBy)

  def getPageMetric(payload: GetMetricPayload): Future[Metric] =
    dbDao.getPageMetric(payload.id, payload.principal)

  def getPageMetrics(payload: GetMetricsPayload): Future[Seq[Metric]] =
    dbDao.getPageMetrics(payload.ids, payload.principal)

}
