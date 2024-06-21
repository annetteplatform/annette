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

package biz.lobachev.annette.cms.impl.blogs.post.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import biz.lobachev.annette.cms.api.common.article.PublicationStatus.PublicationStatus
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.common.article.{Metric, PublicationStatus}
import biz.lobachev.annette.cms.api.content.{ContentTypes, Widget}
import biz.lobachev.annette.cms.impl.blogs.post.PostEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json

import java.time.OffsetDateTime
import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}

private[impl] class PostDbDao(
  override val session: CassandraSession
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private implicit val publicationStatusEncoder = genericStringEncoder[PublicationStatus]
  private implicit val publicationStatusDecoder = genericStringDecoder[PublicationStatus](PublicationStatus.withName)

  private implicit val contentTypeEncoder = genericStringEncoder[ContentType]
  private implicit val contentTypeDecoder = genericStringDecoder[ContentType](ContentTypes.withName)

  private val postSchema       = quote(querySchema[PostRecord]("posts"))
  private val postWidgetSchema = quote(querySchema[PostWidgetRecord]("post_widgets"))
  private val postTargetSchema = quote(querySchema[PostTargetRecord]("post_targets"))
  private val postLikeSchema   = quote(querySchema[PostLikeRecord]("post_likes"))
  private val postViewSchema   = quote(querySchema[PostViewRecord]("post_views"))

  private implicit val insertPostMeta       = insertMeta[PostRecord]()
  private implicit val updatePostMeta       = updateMeta[PostRecord](_.id)
  private implicit val insertPostTargetMeta = insertMeta[PostTargetRecord]()
  private implicit val updatePostViewMeta   = updateMeta[PostViewRecord](_.postId, _.principal)
  touch(publicationStatusEncoder)
  touch(publicationStatusDecoder)
  touch(contentTypeEncoder)
  touch(contentTypeDecoder)
  touch(insertPostMeta)
  touch(updatePostMeta)
  touch(insertPostTargetMeta)
  touch(updatePostViewMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("posts")
               .column("id", Text, true)
               .column("blog_id", Text)
               .column("featured", Boolean)
               .column("author_id", Text)
               .column("title", Text)
               .column("publication_status", Text)
               .column("publication_timestamp", Timestamp)
               .column("intro_content_settings", Text)
               .column("intro_content_order", List(Text))
               .column("post_content_settings", Text)
               .column("post_content_order", List(Text))
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )

      _ <- session.executeCreateTable(
             CassandraTableBuilder("post_widgets")
               .column("post_id", Text)
               .column("content_type", Text)
               .column("widget_id", Text)
               .column("widget_type", Text)
               .column("data", Text)
               .column("index_data", Text)
               .withPrimaryKey("post_id", "content_type", "widget_id")
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("post_targets")
               .column("post_id", Text)
               .column("principal", Text)
               .withPrimaryKey("post_id", "principal")
               .build
           )

      _ <- session.executeCreateTable(
             CassandraTableBuilder("post_likes")
               .column("post_id", Text)
               .column("principal", Text)
               .withPrimaryKey("post_id", "principal")
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("post_views")
               .column("post_id", Text)
               .column("principal", Text)
               .column("views", Counter)
               .withPrimaryKey("post_id", "principal")
               .build
           )

    } yield Done
  }

  def createPost(event: PostEntity.PostCreated) = {
    val postRecord = event
      .into[PostRecord]
      .withFieldComputed(_.introContentSettings, _.introContent.settings)
      .withFieldComputed(_.introContentOrder, _.introContent.widgetOrder.toList)
      .withFieldComputed(_.postContentSettings, _.content.settings)
      .withFieldComputed(_.postContentOrder, _.content.widgetOrder.toList)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(postSchema.insert(lift(postRecord)))
      _ <- Source(event.introContent.widgets.values.toSeq)
             .mapAsync(1) { widget =>
               val postWidgetRecord = widget
                 .into[PostWidgetRecord]
                 .withFieldConst(_.postId, event.id)
                 .withFieldConst(_.contentType, ContentTypes.Intro)
                 .withFieldComputed(_.widgetId, _.id)
                 .transform
               ctx.run(postWidgetSchema.insert(lift(postWidgetRecord)))
             }
             .runWith(Sink.ignore)
      _ <- Source(event.content.widgets.values.toSeq)
             .mapAsync(1) { widget =>
               val postWidgetRecord = widget
                 .into[PostWidgetRecord]
                 .withFieldConst(_.postId, event.id)
                 .withFieldConst(_.contentType, ContentTypes.Post)
                 .withFieldComputed(_.widgetId, _.id)
                 .transform
               ctx.run(postWidgetSchema.insert(lift(postWidgetRecord)))
             }
             .runWith(Sink.ignore)
      _ <- Source(event.targets)
             .mapAsync(1) { target =>
               val postTargetRecord = PostTargetRecord(
                 postId = event.id,
                 principal = target
               )
               ctx.run(postTargetSchema.insert(lift(postTargetRecord)))
             }
             .runWith(Sink.ignore)
    } yield Done
  }

  def updatePostFeatured(event: PostEntity.PostFeaturedUpdated) =
    ctx.run(
      postSchema
        .filter(_.id == lift(event.id))
        .update(
          _.featured  -> lift(event.featured),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updatePostAuthor(event: PostEntity.PostAuthorUpdated) =
    ctx.run(
      postSchema
        .filter(_.id == lift(event.id))
        .update(
          _.authorId  -> lift(event.authorId),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updatePostTitle(event: PostEntity.PostTitleUpdated) =
    ctx.run(
      postSchema
        .filter(_.id == lift(event.id))
        .update(
          _.title     -> lift(event.title),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updateContentSettings(event: PostEntity.ContentSettingsUpdated) =
    for {
      _ <- (event.contentType: @unchecked) match {
             case ContentTypes.Intro =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.introContentSettings -> lift(event.settings),
                     _.updatedAt            -> lift(event.updatedAt),
                     _.updatedBy            -> lift(event.updatedBy)
                   )
               )
             case ContentTypes.Post  =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.postContentSettings -> lift(event.settings),
                     _.updatedAt           -> lift(event.updatedAt),
                     _.updatedBy           -> lift(event.updatedBy)
                   )
               )
           }
    } yield Done

  def updatePostWidget(event: PostEntity.PostWidgetUpdated) = {
    val postWidget = event.widget
      .into[PostWidgetRecord]
      .withFieldConst(_.postId, event.id)
      .withFieldConst(_.contentType, event.contentType)
      .withFieldComputed(_.widgetId, _.id)
      .transform
    for {
      _ <- (event.contentType: @unchecked) match {
             case ContentTypes.Intro =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.introContentOrder -> lift(event.widgetOrder.toList),
                     _.updatedAt         -> lift(event.updatedAt),
                     _.updatedBy         -> lift(event.updatedBy)
                   )
               )
             case ContentTypes.Post  =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.postContentOrder -> lift(event.widgetOrder.toList),
                     _.updatedAt        -> lift(event.updatedAt),
                     _.updatedBy        -> lift(event.updatedBy)
                   )
               )
           }
      _ <- ctx.run(postWidgetSchema.insert(lift(postWidget)))
    } yield Done
  }

  def changeWidgetOrder(event: PostEntity.WidgetOrderChanged) =
    for {
      _ <- (event.contentType: @unchecked) match {
             case ContentTypes.Intro =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.introContentOrder -> lift(event.widgetOrder.toList),
                     _.updatedAt         -> lift(event.updatedAt),
                     _.updatedBy         -> lift(event.updatedBy)
                   )
               )
             case ContentTypes.Post  =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.postContentOrder -> lift(event.widgetOrder.toList),
                     _.updatedAt        -> lift(event.updatedAt),
                     _.updatedBy        -> lift(event.updatedBy)
                   )
               )
           }
    } yield Done

  def deleteWidget(event: PostEntity.WidgetDeleted) =
    for {
      _ <- (event.contentType: @unchecked) match {
             case ContentTypes.Intro =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.introContentOrder -> lift(event.widgetOrder.toList),
                     _.updatedAt         -> lift(event.updatedAt),
                     _.updatedBy         -> lift(event.updatedBy)
                   )
               )
             case ContentTypes.Post  =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.postContentOrder -> lift(event.widgetOrder.toList),
                     _.updatedAt        -> lift(event.updatedAt),
                     _.updatedBy        -> lift(event.updatedBy)
                   )
               )
           }
      _ <- ctx.run(
             postWidgetSchema
               .filter(r =>
                 r.postId == lift(event.id) &&
                   r.contentType == lift(event.contentType) &&
                   r.widgetId == lift(event.widgetId)
               )
               .delete
           )
    } yield Done

  def updatePostPublicationTimestamp(event: PostEntity.PostPublicationTimestampUpdated) =
    ctx.run(
      postSchema
        .filter(_.id == lift(event.id))
        .update(
          _.publicationTimestamp -> lift(event.publicationTimestamp),
          _.updatedAt            -> lift(event.updatedAt),
          _.updatedBy            -> lift(event.updatedBy)
        )
    )

  def publishPost(event: PostEntity.PostPublished) =
    ctx.run(
      postSchema
        .filter(_.id == lift(event.id))
        .update(
          _.publicationStatus    -> lift(PublicationStatus.Published),
          _.publicationTimestamp -> lift(Option(event.publicationTimestamp)),
          _.updatedAt            -> lift(event.updatedAt),
          _.updatedBy            -> lift(event.updatedBy)
        )
    )

  def unpublishPost(event: PostEntity.PostUnpublished) =
    ctx.run(
      postSchema
        .filter(_.id == lift(event.id))
        .update(
          _.publicationStatus -> lift(PublicationStatus.Draft),
          _.updatedAt         -> lift(event.updatedAt),
          _.updatedBy         -> lift(event.updatedBy)
        )
    )

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned) =
    for {
      _ <- ctx.run(
             postTargetSchema.insert(
               lift(
                 PostTargetRecord(
                   postId = event.id,
                   principal = event.principal
                 )
               )
             )
           )

      _ <- ctx.run(
             postSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned) =
    for {
      _ <- ctx.run(
             postTargetSchema
               .filter(r =>
                 r.postId == lift(event.id) &&
                   r.principal == lift(event.principal)
               )
               .delete
           )

      _ <- ctx.run(
             postSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def deletePost(event: PostEntity.PostDeleted) =
    for {
      _ <- ctx.run(postSchema.filter(_.id == lift(event.id)).delete)
      _ <- ctx.run(postWidgetSchema.filter(_.postId == lift(event.id)).delete)
      _ <- ctx.run(postTargetSchema.filter(_.postId == lift(event.id)).delete)
      _ <- ctx.run(postLikeSchema.filter(_.postId == lift(event.id)).delete)
      _ <- ctx.run(postViewSchema.filter(_.postId == lift(event.id)).delete)
    } yield Done

  def getPost(
    id: PostId,
    withIntro: Boolean,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Option[Post]] =
    for {
      maybeEntity       <- ctx
                             .run(postSchema.filter(_.id == lift(id)))
                             .map(_.headOption)
      maybeIntroWidgets <- if (withIntro)
                             maybeEntity
                               .map(_ => getWidgets(id, ContentTypes.Intro).map(Some(_)))
                               .getOrElse(Future.successful(None))
                           else Future.successful(None)
      maybePostWidgets  <- if (withContent)
                             maybeEntity
                               .map(_ => getWidgets(id, ContentTypes.Post).map(Some(_)))
                               .getOrElse(Future.successful(None))
                           else Future.successful(None)
      maybeTargets      <- if (withTargets)
                             maybeEntity.map(_ => getPostTargets(id).map(Some(_))).getOrElse(Future.successful(None))
                           else Future.successful(None)
    } yield maybeEntity.map(
      _.toPost(
        maybeIntroWidgets,
        maybePostWidgets,
        maybeTargets
      )
    )

  private def getWidgets(id: PostId, contentType: ContentType): Future[Map[String, Widget]] =
    ctx
      .run(
        postWidgetSchema
          .filter(r =>
            r.postId == lift(id) &&
              r.contentType == lift(contentType)
          )
          .map(r =>
            (
              r.widgetId,   // 1
              r.widgetType, // 2
              r.data        // 3
            )
          )
      )
      .map(
        _.map(c =>
          c._1 ->
            Widget(
              id = c._1,
              widgetType = c._2,
              data = Json.parse(c._3),
              indexData = None
            )
        ).toMap
      )

  private def getPostTargets(id: PostId): Future[Set[AnnettePrincipal]] =
    ctx
      .run(postTargetSchema.filter(_.postId == lift(id)).map(_.principal))
      .map(_.toSet)

  def getPosts(
    ids: Set[PostId],
    withIntro: Boolean,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Seq[Post]] =
    Source(ids)
      .mapAsync(1)(id => getPost(id, withIntro, withContent, withTargets))
      .runWith(Sink.seq)
      .map(_.flatten)

  def canAccessToPost(id: PostId, principals: Set[AnnettePrincipal]): Future[Boolean] =
    for {
      maybeCount <- ctx
                      .run(
                        postTargetSchema
                          .filter(b =>
                            b.postId == lift(id) &&
                              liftQuery(principals).contains(b.principal)
                          )
                          .size
                      )
    } yield maybeCount.map(_ > 0).getOrElse(false)

  def getPostViews(payload: GetPostViewsPayload): Future[Seq[Post]] =
    for {
      allowedPostIds    <- getAllowedPostIds(payload.ids, payload.principals + payload.directPrincipal)
      postViews         <- if (payload.withContent) getPosts(allowedPostIds, true, true, false)
                           else getPosts(allowedPostIds, true, false, false)
      publishedPostViews = postViews.filter(post =>
                             post.publicationStatus == PublicationStatus.Published &&
                               post.publicationTimestamp.map(_.compareTo(OffsetDateTime.now) <= 0).getOrElse(true)
                           )
      metrics           <- getPostMetrics(publishedPostViews.map(_.id), payload.directPrincipal)
      metricsMap         = metrics.map(a => a.id -> a).toMap

    } yield publishedPostViews
      .map(pv => pv.copy(metric = metricsMap.get(pv.id)))

  private def getAllowedPostIds(ids: Set[PostId], principals: Set[AnnettePrincipal]): Future[Set[String]] =
    ctx
      .run(
        postTargetSchema
          .filter(b =>
            liftQuery(ids).contains(b.postId) &&
              liftQuery(principals).contains(b.principal)
          )
          .map(_.postId)
      )
      .map(_.toSet)

  // ***************************** metrics update *****************************

  def viewPost(id: PostId, principal: AnnettePrincipal): Future[Done] =
    session
      .executeWrite(
        """UPDATE post_views SET views = views + 1
   WHERE post_id = ? AND principal = ? """,
        id,
        principal.code
      )
      .map(_ => Done)

  def likePost(id: PostId, principal: AnnettePrincipal): Future[Done] =
    ctx.run(
      postLikeSchema.insert(
        lift(
          PostLikeRecord(
            postId = id,
            principal = principal
          )
        )
      )
    )

  def unlikePost(id: PostId, principal: AnnettePrincipal): Future[Done] =
    ctx.run(postLikeSchema.filter(r => r.postId == lift(id) && r.principal == lift(principal)).delete)

  // ***************************** metrics *****************************

  def getPostMetrics(ids: Seq[PostId], principal: AnnettePrincipal): Future[Seq[Metric]] =
    Source(ids)
      .mapAsync(1)(id => getPostMetric(id, principal))
      .runWith(Sink.seq)

  def getPostMetric(id: PostId, principal: AnnettePrincipal): Future[Metric] =
    for {
      views     <- getPostViewsCount(id)
      likes     <- getPostLikesCount(id)
      likedByMe <- getPostLikedByMe(id, principal)
    } yield Metric(id, views, likes, likedByMe)

  private def getPostViewsCount(id: PostId): Future[Int] =
    for {
      maybeCount <- ctx
                      .run(
                        postViewSchema
                          .filter(b => b.postId == lift(id))
                          .size
                      )
    } yield maybeCount.map(_.toInt).getOrElse(0)

  private def getPostLikesCount(id: PostId): Future[Int] =
    for {
      maybeCount <- ctx
                      .run(
                        postLikeSchema
                          .filter(b => b.postId == lift(id))
                          .size
                      )
    } yield maybeCount.map(_.toInt).getOrElse(0)

  private def getPostLikedByMe(id: PostId, principal: AnnettePrincipal): Future[Boolean] =
    for {
      maybeLike <- ctx
                     .run(
                       postLikeSchema
                         .filter(b => b.postId == lift(id) && b.principal == lift(principal))
                         .map(_.postId)
                     )
                     .map(_.headOption)
    } yield maybeLike.map(_ => true).getOrElse(false)

}
