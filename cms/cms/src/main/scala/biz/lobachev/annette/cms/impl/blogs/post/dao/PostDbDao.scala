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
import biz.lobachev.annette.cms.api.blogs.post.PublicationStatus.PublicationStatus
import biz.lobachev.annette.cms.api.blogs.post.{ContentTypes, _}
import biz.lobachev.annette.cms.api.content.WidgetContent
import biz.lobachev.annette.cms.impl.blogs.post.PostEntity
import ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

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

  private implicit val postContentEncoder = genericJsonEncoder[PostContent]
  private implicit val postContentDecoder = genericJsonDecoder[PostContent]

  private val postSchema       = quote(querySchema[PostRecord]("posts"))
  private val postWidgetSchema = quote(querySchema[PostWidgetRecord]("post_widgets"))
  private val postTargetSchema = quote(querySchema[PostTargetRecord]("post_targets"))
  private val postLikeSchema   = quote(querySchema[PostLikeRecord]("post_likes"))
  private val postViewSchema   = quote(querySchema[PostViewRecord]("post_views"))

  private implicit val insertPostMeta       = insertMeta[PostRecord]()
  private implicit val updatePostMeta       = updateMeta[PostRecord](_.id)
  private implicit val insertPostTargetMeta = insertMeta[PostTargetRecord]()
  private implicit val updatePostViewMeta   = updateMeta[PostViewRecord](_.postId, _.principal)
  println(publicationStatusEncoder.toString)
  println(publicationStatusDecoder.toString)
  println(contentTypeEncoder.toString)
  println(contentTypeDecoder.toString)
  println(postContentEncoder.toString)
  println(postContentDecoder.toString)
  println(insertPostMeta.toString)
  println(updatePostMeta.toString)
  println(insertPostTargetMeta.toString)
  println(updatePostViewMeta.toString)

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
               .column("intro_content_order", List(Text))
               .column("post_content_order", List(Text))
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )

      _ <- session.executeCreateTable(
             CassandraTableBuilder("post_widgets")
               .column("post_id", Text)
               .column("content_type", Text)
               .column("widget_content_id", Text)
               .column("widget_type", Text)
               .column("data", Text)
               .column("index_data", Text)
               .withPrimaryKey("post_id", "content_type", "widget_content_id")
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
             CassandraTableBuilder("post_media")
               .column("post_id", Text)
               .column("media_id", Text)
               .column("filename", Text)
               .withPrimaryKey("post_id", "media_id")
               .build
           )

      _ <- session.executeCreateTable(
             CassandraTableBuilder("post_docs")
               .column("post_id", Text)
               .column("doc_id", Text)
               .column("name", Text)
               .column("filename", Text)
               .withPrimaryKey("post_id", "doc_id")
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
      .withFieldComputed(_.introContentOrder, _.introContent.contentOrder.toList)
      .withFieldComputed(_.postContentOrder, _.content.contentOrder.toList)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(postSchema.insert(lift(postRecord)))
      _ <- Source(event.introContent.content.values.toSeq)
             .mapAsync(1) { widget =>
               val postWidgetRecord = widget
                 .into[PostWidgetRecord]
                 .withFieldConst(_.postId, event.id)
                 .withFieldConst(_.contentType, ContentTypes.Intro)
                 .withFieldComputed(_.widgetContentId, _.id)
                 .transform
               ctx.run(postWidgetSchema.insert(lift(postWidgetRecord)))
             }
             .runWith(Sink.ignore)
      _ <- Source(event.content.content.values.toSeq)
             .mapAsync(1) { widget =>
               val postWidgetRecord = widget
                 .into[PostWidgetRecord]
                 .withFieldConst(_.postId, event.id)
                 .withFieldConst(_.contentType, ContentTypes.Post)
                 .withFieldComputed(_.widgetContentId, _.id)
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

  def updatePostWidgetContent(event: PostEntity.PostWidgetContentUpdated) = {
    val postWidget = event.widgetContent
      .into[PostWidgetRecord]
      .withFieldConst(_.postId, event.id)
      .withFieldConst(_.contentType, event.contentType)
      .withFieldComputed(_.widgetContentId, _.id)
      .transform
    for {
      _ <- event.contentType match {
             case ContentTypes.Intro =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.introContentOrder -> lift(event.contentOrder.toList),
                     _.updatedAt         -> lift(event.updatedAt),
                     _.updatedBy         -> lift(event.updatedBy)
                   )
               )
             case ContentTypes.Post  =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.postContentOrder -> lift(event.contentOrder.toList),
                     _.updatedAt        -> lift(event.updatedAt),
                     _.updatedBy        -> lift(event.updatedBy)
                   )
               )
           }
      _ <- ctx.run(postWidgetSchema.insert(lift(postWidget)))
    } yield Done
  }

  def changeWidgetContentOrder(event: PostEntity.WidgetContentOrderChanged) =
    for {
      _ <- event.contentType match {
             case ContentTypes.Intro =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.introContentOrder -> lift(event.contentOrder.toList),
                     _.updatedAt         -> lift(event.updatedAt),
                     _.updatedBy         -> lift(event.updatedBy)
                   )
               )
             case ContentTypes.Post  =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.postContentOrder -> lift(event.contentOrder.toList),
                     _.updatedAt        -> lift(event.updatedAt),
                     _.updatedBy        -> lift(event.updatedBy)
                   )
               )
           }
    } yield Done

  def deleteWidgetContent(event: PostEntity.WidgetContentDeleted) =
    for {
      _ <- event.contentType match {
             case ContentTypes.Intro =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.introContentOrder -> lift(event.contentOrder.toList),
                     _.updatedAt         -> lift(event.updatedAt),
                     _.updatedBy         -> lift(event.updatedBy)
                   )
               )
             case ContentTypes.Post  =>
               ctx.run(
                 postSchema
                   .filter(_.id == lift(event.id))
                   .update(
                     _.postContentOrder -> lift(event.contentOrder.toList),
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
                   r.widgetContentId == lift(event.widgetContentId)
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

  def getPostById(id: PostId): Future[Option[Post]] =
    for {
      maybeEntity         <- ctx
                               .run(postSchema.filter(_.id == lift(id)))
                               .map(_.headOption)
      introWidgetContents <- maybeEntity
                               .map(_ => getPostWidgets(id, ContentTypes.Intro))
                               .getOrElse(Future.successful(Map.empty[String, WidgetContent]))
      postWidgetContents  <- maybeEntity
                               .map(_ => getPostWidgets(id, ContentTypes.Post))
                               .getOrElse(Future.successful(Map.empty[String, WidgetContent]))
      targets             <- maybeEntity.map(_ => getPostTargets(id)).getOrElse(Future.successful(Set.empty[AnnettePrincipal]))
    } yield maybeEntity.map(
      _.toPost(
        introWidgetContents,
        postWidgetContents,
        targets
      )
    )

  private def getPostWidgets(id: PostId, contentType: ContentType): Future[Map[String, WidgetContent]] =
    ctx
      .run(
        postWidgetSchema.filter(r =>
          r.postId == lift(id) &&
            r.contentType == lift(contentType)
        )
      )
      .map(_.map(c => c.widgetContentId -> c.toWidgetContent).toMap)

  private def getPostTargets(id: PostId): Future[Set[AnnettePrincipal]]                                =
    ctx
      .run(postTargetSchema.filter(_.postId == lift(id)).map(_.principal))
      .map(_.toSet)

  def getPostAnnotationById(id: PostId): Future[Option[PostAnnotation]] =
    for {
      maybeEntity         <- ctx
                               .run(
                                 postSchema
                                   .filter(_.id == lift(id))
                                   .map(r =>
                                     (
                                       r.id,                   // 1
                                       r.blogId,               // 2
                                       r.featured,             // 3
                                       r.authorId,             // 4
                                       r.title,                // 5
                                       r.introContentOrder,    // 6
                                       r.publicationStatus,    // 7
                                       r.publicationTimestamp, // 8
                                       r.updatedBy,            // 9
                                       r.updatedAt             // 10
                                     )
                                   )
                               )
                               .map(_.headOption)
      introWidgetContents <- maybeEntity
                               .map(_ => getPostWidgets(id, ContentTypes.Intro))
                               .getOrElse(Future.successful(Map.empty[String, WidgetContent]))
    } yield maybeEntity
      .map(r =>
        PostAnnotation(
          id = r._1,
          blogId = r._2,
          featured = r._3,
          authorId = r._4,
          title = r._5,
          introContent = r._6.map(c => introWidgetContents.get(c)).flatten,
          publicationStatus = r._7,
          publicationTimestamp = r._8,
          updatedBy = r._9,
          updatedAt = r._10
        )
      )

  def getPostsById(ids: Set[PostId]): Future[Seq[Post]] =
    Source(ids)
      .mapAsync(1)(getPostById)
      .runWith(Sink.seq)
      .map(_.flatten)

  private def getPostWidgetsMap(
    ids: Set[PostId],
    contentType: ContentType
  ): Future[Map[String, Map[String, WidgetContent]]]                        =
    ctx
      .run(
        postWidgetSchema.filter(r =>
          liftQuery(ids).contains(r.postId) &&
            r.contentType == lift(contentType)
        )
      )
      .map(_.groupBy(_.postId).map { case k -> v => k -> v.map(c => c.widgetContentId -> c.toWidgetContent).toMap })

  def getPostAnnotationsById(ids: Set[PostId]): Future[Seq[PostAnnotation]] =
    for {
      posts          <- ctx
                          .run(
                            postSchema
                              .filter(b => liftQuery(ids).contains(b.id))
                              .map(r =>
                                (
                                  r.id,                   // 1
                                  r.blogId,               // 2
                                  r.featured,             // 3
                                  r.authorId,             // 4
                                  r.title,                // 5
                                  r.introContentOrder,    // 6
                                  r.publicationStatus,    // 7
                                  r.publicationTimestamp, // 8
                                  r.updatedBy,            // 9
                                  r.updatedAt             // 10
                                )
                              )
                          )

      postWidgetsMap <- getPostWidgetsMap(ids, ContentTypes.Intro)

    } yield posts.map { r =>
      val postIntroWidgets = postWidgetsMap.get(r._1).getOrElse(Map.empty)
      PostAnnotation(
        id = r._1,
        blogId = r._2,
        featured = r._3,
        authorId = r._4,
        title = r._5,
        introContent = r._6.map(c => postIntroWidgets.get(c)).flatten,
        publicationStatus = r._7,
        publicationTimestamp = r._8,
        updatedBy = r._9,
        updatedAt = r._10
      )
    }

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

  def getPostViewsById(payload: GetPostViewsPayload): Future[Seq[PostView]] =
    for {
      allowedPostIds    <- getAllowedPostIds(payload.ids, payload.principals + payload.directPrincipal)
      postViews         <- if (payload.withContent) getPostViewsWithContent(allowedPostIds)
                           else getPostViewsWithoutContent(allowedPostIds)
      publishedPostViews = postViews.filter(post =>
                             post.publicationStatus == PublicationStatus.Published &&
                               post.publicationTimestamp.map(_.compareTo(OffsetDateTime.now) <= 0).getOrElse(true)
                           )
      metrics           <- getPostMetricsById(publishedPostViews.map(_.id).toSet, payload.directPrincipal)
      metricsMap         = metrics.map(a => a.id -> a).toMap

    } yield publishedPostViews
      .map(pv => pv.copy(metric = metricsMap.get(pv.id)))

  def getPostViewsWithContent(ids: Set[String]): Future[Seq[PostView]] =
    for {
      posts           <- ctx.run(
                           postSchema
                             .filter(b => liftQuery(ids).contains(b.id))
                         )
      introWidgetsMap <- getPostWidgetsMap(ids, ContentTypes.Intro)
      postWidgetsMap  <- getPostWidgetsMap(ids, ContentTypes.Post)
    } yield posts.map { r =>
      val introWidgets = introWidgetsMap.get(r.id).getOrElse(Map.empty)
      val postWidgets  = postWidgetsMap.get(r.id).getOrElse(Map.empty)
      r.toPostView(introWidgets, postWidgets)
    }

  def getPostViewsWithoutContent(ids: Set[String]): Future[Seq[PostView]] =
    for {
      posts           <- ctx
                           .run(
                             postSchema
                               .filter(b => liftQuery(ids).contains(b.id))
                               .map(r =>
                                 (
                                   r.id,                   // 1
                                   r.blogId,               // 2
                                   r.featured,             // 3
                                   r.authorId,             // 4
                                   r.title,                // 5
                                   r.introContentOrder,    // 6
                                   r.publicationStatus,    // 7
                                   r.publicationTimestamp, // 8
                                   r.updatedBy,            // 9
                                   r.updatedAt             // 10
                                 )
                               )
                           )
      introWidgetsMap <- getPostWidgetsMap(ids, ContentTypes.Intro)
    } yield posts.map { r =>
      val introWidgets = introWidgetsMap.get(r._1).getOrElse(Map.empty)
      PostView(
        id = r._1,
        blogId = r._2,
        featured = r._3,
        authorId = r._4,
        title = r._5,
        introContent = r._6.map(c => introWidgets.get(c)).flatten,
        content = None,
        publicationStatus = r._7,
        publicationTimestamp = r._8,
        metric = None,
        updatedBy = r._9,
        updatedAt = r._10
      )
    }

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

  def getPostMetricsById(ids: Set[PostId], principal: AnnettePrincipal): Future[Seq[PostMetric]] =
    Source(ids)
      .mapAsync(1)(id => getPostMetricById(id, principal))
      .runWith(Sink.seq)

  def getPostMetricById(id: PostId, principal: AnnettePrincipal): Future[PostMetric] =
    for {
      views     <- getPostViewsCountById(id)
      likes     <- getPostLikesCountById(id)
      likedByMe <- getPostLikedByMeById(id, principal)
    } yield PostMetric(id, views, likes, likedByMe)

  private def getPostViewsCountById(id: PostId): Future[Int] =
    for {
      maybeCount <- ctx
                      .run(
                        postViewSchema
                          .filter(b => b.postId == lift(id))
                          .size
                      )
    } yield maybeCount.map(_.toInt).getOrElse(0)

  private def getPostLikesCountById(id: PostId): Future[Int] =
    for {
      maybeCount <- ctx
                      .run(
                        postLikeSchema
                          .filter(b => b.postId == lift(id))
                          .size
                      )
    } yield maybeCount.map(_.toInt).getOrElse(0)

  private def getPostLikedByMeById(id: PostId, principal: AnnettePrincipal): Future[Boolean] =
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
