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

package biz.lobachev.annette.ignition.cms.loaders

import java.time.OffsetDateTime
import org.apache.pekko.Done
import org.apache.pekko.stream.Materializer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.common.article.{PublishPayload, UpdateAuthorPayload, UpdatePublicationTimestampPayload, UpdateTitlePayload}
import biz.lobachev.annette.cms.api.content.{
  Content,
  DeleteWidgetPayload,
  UpdateContentSettingsPayload,
  UpdateWidgetPayload,
  Widget
}
import biz.lobachev.annette.cms.api.pages.page.{CreatePagePayload, PageAlreadyExist}
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, SystemPrincipal}
import biz.lobachev.annette.ignition.cms.loaders.data.PageData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PageEntityLoader(
  service: CmsService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[PageData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[PageData] = PageData.format

  override val name: String = "page"

  def loadItem(item: PageData): Future[LoadStatus] =
    parseTimestamp(item.publicationTimestamp) match {
      case Left(error) =>
        Future.successful(error)
      case Right(publishedAt) =>
        val published     = item.publicationStatus.forall(_.equalsIgnoreCase("published"))
        val createdBy     = SystemPrincipal()
        val createPayload = CreatePagePayload(
          id = item.id,
          spaceId = item.spaceId,
          authorId = AnnettePrincipal(item.authorId),
          title = item.title,
          content = pageContent(item),
          createdBy = createdBy
        )
        service
          .createPage(createPayload)
          .flatMap(_ => publish(item, published, publishedAt, createdBy))
          .map(_ => LoadOk)
          .recoverWith {
            case PageAlreadyExist(_) if config.mode == UpsertMode =>
              updatePage(item, published, publishedAt, createdBy)
            case th                                               => Future.failed(th)
          }
    }

  // Author, title and content are synced to the demo data; publication status is
  // re-applied. Content convergence includes pruning widgets that are no longer
  // part of the data.
  private def updatePage(
    item: PageData,
    published: Boolean,
    publishedAt: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal
  ): Future[LoadStatus] =
    for {
      _ <- service.updatePageAuthor(UpdateAuthorPayload(item.id, AnnettePrincipal(item.authorId), updatedBy))
      _ <- service.updatePageTitle(UpdateTitlePayload(item.id, item.title, updatedBy))
      _ <- syncContent(item, updatedBy)
      _ <- publish(item, published, publishedAt, updatedBy)
    } yield LoadOk

  private def syncContent(item: PageData, updatedBy: AnnettePrincipal): Future[Done] = {
    def update(widget: Widget, order: Int): Future[Done] =
      service.updatePageWidget(UpdateWidgetPayload(item.id, None, widget, Some(order), updatedBy)).map(_ => Done)
    def remove(widgetId: String): Future[Done] =
      service.deletePageWidget(DeleteWidgetPayload(item.id, None, widgetId, updatedBy)).map(_ => Done)
    service
      .getPage(item.id, None, Some(true), None)
      .flatMap { current =>
        for {
          _ <- service.updatePageContentSettings(
                 UpdateContentSettingsPayload(item.id, None, pageContent(item).settings, updatedBy)
               )
          _ <- ContentOps.replaceWidgets(update, remove, pageContent(item), current.content)
        } yield Done
      }
  }

  private def pageContent(item: PageData): Content = ContentOps.prepare(item.content.getOrElse(ContentOps.empty))

  private def publish(
    item: PageData,
    published: Boolean,
    publishedAt: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal
  ): Future[Done] =
    for {
      _ <- publishedAt match {
             case Some(timestamp) =>
               service
                 .updatePagePublicationTimestamp(
                   UpdatePublicationTimestampPayload(item.id, Some(timestamp), updatedBy)
                 )
                 .map(_ => Done)
             case None            => Future.successful(Done)
           }
      _ <- if (published) service.publishPage(PublishPayload(item.id, updatedBy)).map(_ => Done)
           else Future.successful(Done)
    } yield Done

  private def parseTimestamp(value: Option[String]): Either[LoadStatus, Option[OffsetDateTime]] =
    value match {
      case None     => Right(None)
      case Some(ts) =>
        Try(OffsetDateTime.parse(ts)) match {
          case Success(parsed)    => Right(Some(parsed))
          case Failure(exception) =>
            Left(LoadFailed(s"Invalid publicationTimestamp '$ts': ${exception.getMessage}"))
        }
    }

}
