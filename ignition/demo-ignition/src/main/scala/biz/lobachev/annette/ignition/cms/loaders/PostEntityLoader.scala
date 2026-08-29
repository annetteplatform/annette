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
import biz.lobachev.annette.cms.api.common.article.{
  PublishPayload,
  UpdateAuthorPayload,
  UpdatePublicationTimestampPayload,
  UpdateTitlePayload
}
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
      case Left(error) => Future.successful(error)
      case Right(publishedAt) =>
        val published     = item.publicationStatus.forall(_.equalsIgnoreCase("published"))
        val featured      = item.featured.getOrElse(false)
        val createdBy     = SystemPrincipal()
        val createPayload = CreatePostPayload(
          id = item.id,
          blogId = item.blogId,
          featured = featured,
          authorId = AnnettePrincipal(item.authorId),
          title = item.title,
          introContent = ContentOps.withDerivedIndexData(item.introContent.getOrElse(ContentOps.empty)),
          content = ContentOps.withDerivedIndexData(item.content.getOrElse(ContentOps.empty)),
          createdBy = createdBy
        )
        service
          .createPost(createPayload)
          .flatMap(_ => publish(item, published, publishedAt, createdBy))
          .map(_ => LoadOk)
          .recoverWith {
            case PostAlreadyExist(_) if config.mode == UpsertMode =>
              updatePost(item, featured, published, publishedAt, createdBy)
            case th                                               => Future.failed(th)
          }
    }

  // Featured flag, author and title are updated in place; publication status is re-applied.
  // Post content created earlier is left untouched (first write wins).
  private def updatePost(
    item: PostData,
    featured: Boolean,
    published: Boolean,
    publishedAt: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal
  ): Future[LoadStatus] =
    for {
      _ <- service.updatePostFeatured(UpdatePostFeaturedPayload(item.id, featured, updatedBy))
      _ <- service.updatePostAuthor(UpdateAuthorPayload(item.id, AnnettePrincipal(item.authorId), updatedBy))
      _ <- service.updatePostTitle(UpdateTitlePayload(item.id, item.title, updatedBy))
      _ <- publish(item, published, publishedAt, updatedBy)
    } yield LoadOk

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
          case Success(parsed)     => Right(Some(parsed))
          case Failure(exception)  =>
            Left(LoadFailed(s"Invalid publicationTimestamp '$ts': ${exception.getMessage}"))
        }
    }

}
