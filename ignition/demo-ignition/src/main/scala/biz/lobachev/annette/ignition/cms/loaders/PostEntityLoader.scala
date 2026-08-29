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
import biz.lobachev.annette.cms.api.blogs.post.{CreatePostPayload, PostAlreadyExist, UpdatePostFeaturedPayload}
import biz.lobachev.annette.cms.api.common.article.{PublishPayload, UpdateAuthorPayload, UpdatePublicationTimestampPayload, UpdateTitlePayload}
import biz.lobachev.annette.cms.api.content.{Content, ContentTypes, DeleteWidgetPayload, UpdateWidgetPayload, Widget}
import biz.lobachev.annette.cms.api.content.UpdateContentSettingsPayload
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, SystemPrincipal}
import biz.lobachev.annette.ignition.cms.loaders.data.PostData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PostEntityLoader(
  service: CmsService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[PostData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[PostData] = PostData.format

  override val name: String = "post"

  def loadItem(item: PostData): Future[LoadStatus] =
    parseTimestamp(item.publicationTimestamp) match {
      case Left(error) =>
        Future.successful(error)
      case Right(publishedAt) =>
        val published     = item.publicationStatus.forall(_.equalsIgnoreCase("published"))
        val createdBy     = SystemPrincipal()
        val createPayload = CreatePostPayload(
          id = item.id,
          blogId = item.blogId,
          featured = item.featured.getOrElse(false),
          authorId = AnnettePrincipal(item.authorId),
          title = item.title,
          introContent = introContent(item),
          content = mainContent(item),
          createdBy = createdBy
        )
        service
          .createPost(createPayload)
          .flatMap(_ => publish(item, published, publishedAt, createdBy))
          .map(_ => LoadOk)
          .recoverWith {
            case PostAlreadyExist(_) if config.mode == UpsertMode =>
              updatePost(item, published, publishedAt, createdBy)
            case th                                               => Future.failed(th)
          }
    }

  // Featured flag, author, title and content are synced to the demo data; publication
  // status is re-applied. Content convergence includes pruning widgets that are no
  // longer part of the data.
  private def updatePost(
    item: PostData,
    published: Boolean,
    publishedAt: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal
  ): Future[LoadStatus] =
    for {
      _ <- service.updatePostFeatured(UpdatePostFeaturedPayload(item.id, item.featured.getOrElse(false), updatedBy))
      _ <- service.updatePostAuthor(UpdateAuthorPayload(item.id, AnnettePrincipal(item.authorId), updatedBy))
      _ <- service.updatePostTitle(UpdateTitlePayload(item.id, item.title, updatedBy))
      _ <- syncContent(item, updatedBy)
      _ <- publish(item, published, publishedAt, updatedBy)
    } yield LoadOk

  private def syncContent(item: PostData, updatedBy: AnnettePrincipal): Future[Done] = {
    def update(contentType: ContentTypes.ContentType, widget: Widget, order: Int): Future[Done] =
      service
        .updatePostWidget(UpdateWidgetPayload(item.id, Some(contentType), widget, Some(order), updatedBy))
        .map(_ => Done)
    def remove(contentType: ContentTypes.ContentType, widgetId: String): Future[Done] =
      service.deletePostWidget(DeleteWidgetPayload(item.id, Some(contentType), widgetId, updatedBy)).map(_ => Done)
    service
      .getPost(item.id, None, Some(true), Some(true), None)
      .flatMap { current =>
        for {
          _ <- service.updatePostContentSettings(
                 UpdateContentSettingsPayload(item.id, Some(ContentTypes.Intro), introContent(item).settings, updatedBy)
               )
          _ <- service.updatePostContentSettings(
                 UpdateContentSettingsPayload(item.id, Some(ContentTypes.Post), mainContent(item).settings, updatedBy)
               )
          _ <- ContentOps.replaceWidgets(
                 (widget, order) => update(ContentTypes.Intro, widget, order),
                 remove(ContentTypes.Intro, _),
                 introContent(item),
                 current.introContent
               )
          _ <- ContentOps.replaceWidgets(
                 (widget, order) => update(ContentTypes.Post, widget, order),
                 remove(ContentTypes.Post, _),
                 mainContent(item),
                 current.content
               )
        } yield Done
      }
  }

  private def introContent(item: PostData): Content = ContentOps.prepare(item.introContent.getOrElse(ContentOps.empty))

  private def mainContent(item: PostData): Content = ContentOps.prepare(item.content.getOrElse(ContentOps.empty))

  private def publish(
    item: PostData,
    published: Boolean,
    publishedAt: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal
  ): Future[Done] =
    for {
      _ <- publishedAt match {
             case Some(timestamp) =>
               service
                 .updatePostPublicationTimestamp(
                   UpdatePublicationTimestampPayload(item.id, Some(timestamp), updatedBy)
                 )
                 .map(_ => Done)
             case None            => Future.successful(Done)
           }
      _ <- if (published) service.publishPost(PublishPayload(item.id, updatedBy)).map(_ => Done)
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
