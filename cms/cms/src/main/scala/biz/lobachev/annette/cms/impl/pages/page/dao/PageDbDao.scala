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

package biz.lobachev.annette.cms.impl.pages.page.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.cms.api.pages.page.PublicationStatus.PublicationStatus
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.common.WidgetContent
import biz.lobachev.annette.cms.impl.pages.page.PageEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import java.time.OffsetDateTime
import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}

private[impl] class PageDbDao(
  override val session: CassandraSession
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private implicit val publicationStatusEncoder = genericStringEncoder[PublicationStatus]
  private implicit val publicationStatusDecoder = genericStringDecoder[PublicationStatus](PublicationStatus.withName)

  private val pageSchema       = quote(querySchema[PageRecord]("pages"))
  private val pageWidgetSchema = quote(querySchema[PageWidgetRecord]("page_widgets"))
  private val pageTargetSchema = quote(querySchema[PageTargetRecord]("page_targets"))
  private val pageLikeSchema   = quote(querySchema[PageLikeRecord]("page_likes"))
  private val pageViewSchema   = quote(querySchema[PageViewRecord]("page_views"))

  private implicit val insertPageMeta       = insertMeta[PageRecord]()
  private implicit val updatePageMeta       = updateMeta[PageRecord](_.id)
  private implicit val insertPageTargetMeta = insertMeta[PageTargetRecord]()
  private implicit val updatePageViewMeta   = updateMeta[PageViewRecord](_.pageId, _.principal)
  println(publicationStatusEncoder.toString)
  println(publicationStatusDecoder.toString)
  println(insertPageMeta.toString)
  println(updatePageMeta.toString)
  println(insertPageTargetMeta.toString)
  println(updatePageViewMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("pages")
               .column("id", Text, true)
               .column("space_id", Text)
               .column("author_id", Text)
               .column("title", Text)
               .column("publication_status", Text)
               .column("publication_timestamp", Timestamp)
               .column("page_content_order", List(Text))
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )

      _ <- session.executeCreateTable(
             CassandraTableBuilder("page_widgets")
               .column("page_id", Text)
               .column("widget_content_id", Text)
               .column("widget_type", Text)
               .column("data", Text)
               .column("index_data", Text)
               .withPrimaryKey("page_id", "widget_content_id")
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("page_targets")
               .column("page_id", Text)
               .column("principal", Text)
               .withPrimaryKey("page_id", "principal")
               .build
           )

      _ <- session.executeCreateTable(
             CassandraTableBuilder("page_likes")
               .column("page_id", Text)
               .column("principal", Text)
               .withPrimaryKey("page_id", "principal")
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("page_views")
               .column("page_id", Text)
               .column("principal", Text)
               .column("views", Counter)
               .withPrimaryKey("page_id", "principal")
               .build
           )

    } yield Done
  }

  def createPage(event: PageEntity.PageCreated) = {
    val pageRecord = event
      .into[PageRecord]
      .withFieldComputed(_.pageContentOrder, _.content.contentOrder.toList)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(pageSchema.insert(lift(pageRecord)))

      _ <- Source(event.content.content.values.toSeq)
             .mapAsync(1) { widget =>
               val pageWidgetRecord = widget
                 .into[PageWidgetRecord]
                 .withFieldConst(_.pageId, event.id)
                 .withFieldComputed(_.widgetContentId, _.id)
                 .transform
               ctx.run(pageWidgetSchema.insert(lift(pageWidgetRecord)))
             }
             .runWith(Sink.ignore)
      _ <- Source(event.targets)
             .mapAsync(1) { target =>
               val pageTargetRecord = PageTargetRecord(
                 pageId = event.id,
                 principal = target
               )
               ctx.run(pageTargetSchema.insert(lift(pageTargetRecord)))
             }
             .runWith(Sink.ignore)
    } yield Done
  }

  def updatePageAuthor(event: PageEntity.PageAuthorUpdated) =
    ctx.run(
      pageSchema
        .filter(_.id == lift(event.id))
        .update(
          _.authorId  -> lift(event.authorId),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updatePageTitle(event: PageEntity.PageTitleUpdated) =
    ctx.run(
      pageSchema
        .filter(_.id == lift(event.id))
        .update(
          _.title     -> lift(event.title),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updatePageWidgetContent(event: PageEntity.PageWidgetContentUpdated) = {
    val pageWidget = event.widgetContent
      .into[PageWidgetRecord]
      .withFieldConst(_.pageId, event.id)
      .withFieldComputed(_.widgetContentId, _.id)
      .transform
    for {
      _ <- ctx.run(
             pageSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.pageContentOrder -> lift(event.contentOrder.toList),
                 _.updatedAt        -> lift(event.updatedAt),
                 _.updatedBy        -> lift(event.updatedBy)
               )
           )
      _ <- ctx.run(pageWidgetSchema.insert(lift(pageWidget)))
    } yield Done
  }

  def changeWidgetContentOrder(event: PageEntity.WidgetContentOrderChanged) =
    for {
      _ <- ctx.run(
             pageSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.pageContentOrder -> lift(event.contentOrder.toList),
                 _.updatedAt        -> lift(event.updatedAt),
                 _.updatedBy        -> lift(event.updatedBy)
               )
           )
    } yield Done

  def deleteWidgetContent(event: PageEntity.WidgetContentDeleted) =
    for {
      _ <- ctx.run(
             pageSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.pageContentOrder -> lift(event.contentOrder.toList),
                 _.updatedAt        -> lift(event.updatedAt),
                 _.updatedBy        -> lift(event.updatedBy)
               )
           )
      _ <- ctx.run(
             pageWidgetSchema
               .filter(r =>
                 r.pageId == lift(event.id) &&
                   r.widgetContentId == lift(event.widgetContentId)
               )
               .delete
           )
    } yield Done

  def updatePagePublicationTimestamp(event: PageEntity.PagePublicationTimestampUpdated) =
    ctx.run(
      pageSchema
        .filter(_.id == lift(event.id))
        .update(
          _.publicationTimestamp -> lift(event.publicationTimestamp),
          _.updatedAt            -> lift(event.updatedAt),
          _.updatedBy            -> lift(event.updatedBy)
        )
    )

  def publishPage(event: PageEntity.PagePublished) =
    ctx.run(
      pageSchema
        .filter(_.id == lift(event.id))
        .update(
          _.publicationStatus    -> lift(PublicationStatus.Published),
          _.publicationTimestamp -> lift(Option(event.publicationTimestamp)),
          _.updatedAt            -> lift(event.updatedAt),
          _.updatedBy            -> lift(event.updatedBy)
        )
    )

  def unpublishPage(event: PageEntity.PageUnpublished) =
    ctx.run(
      pageSchema
        .filter(_.id == lift(event.id))
        .update(
          _.publicationStatus -> lift(PublicationStatus.Draft),
          _.updatedAt         -> lift(event.updatedAt),
          _.updatedBy         -> lift(event.updatedBy)
        )
    )

  def assignPageTargetPrincipal(event: PageEntity.PageTargetPrincipalAssigned) =
    for {
      _ <- ctx.run(
             pageTargetSchema.insert(
               lift(
                 PageTargetRecord(
                   pageId = event.id,
                   principal = event.principal
                 )
               )
             )
           )

      _ <- ctx.run(
             pageSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def unassignPageTargetPrincipal(event: PageEntity.PageTargetPrincipalUnassigned) =
    for {
      _ <- ctx.run(
             pageTargetSchema
               .filter(r =>
                 r.pageId == lift(event.id) &&
                   r.principal == lift(event.principal)
               )
               .delete
           )

      _ <- ctx.run(
             pageSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def deletePage(event: PageEntity.PageDeleted) =
    for {
      _ <- ctx.run(pageSchema.filter(_.id == lift(event.id)).delete)
      _ <- ctx.run(pageWidgetSchema.filter(_.pageId == lift(event.id)).delete)
      _ <- ctx.run(pageTargetSchema.filter(_.pageId == lift(event.id)).delete)
      _ <- ctx.run(pageLikeSchema.filter(_.pageId == lift(event.id)).delete)
      _ <- ctx.run(pageViewSchema.filter(_.pageId == lift(event.id)).delete)
    } yield Done

  def getPageById(
    id: PageId,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Option[Page]] =
    for {
      maybeEntity             <- ctx
                                   .run(pageSchema.filter(_.id == lift(id)))
                                   .map(_.headOption)
      maybePageWidgetContents <- if (withContent)
                                   maybeEntity
                                     .map(_ => getPageWidgets(id).map(Some(_)))
                                     .getOrElse(Future.successful(None))
                                 else Future.successful(None)
      maybeTargets            <- if (withTargets)
                                   maybeEntity.map(_ => getPageTargets(id).map(Some(_))).getOrElse(Future.successful(None))
                                 else Future.successful(None)
    } yield maybeEntity.map(
      _.toPage(
        maybePageWidgetContents,
        maybeTargets
      )
    )

  private def getPageWidgets(id: PageId): Future[Map[String, WidgetContent]] =
    ctx
      .run(
        pageWidgetSchema.filter(r => r.pageId == lift(id))
      )
      .map(_.map(c => c.widgetContentId -> c.toWidgetContent).toMap)

  private def getPageTargets(id: PageId): Future[Set[AnnettePrincipal]]      =
    ctx
      .run(pageTargetSchema.filter(_.pageId == lift(id)).map(_.principal))
      .map(_.toSet)

  def getPagesById(
    ids: Set[PageId],
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Seq[Page]] =
    Source(ids)
      .mapAsync(1)(id => getPageById(id, withContent, withTargets))
      .runWith(Sink.seq)
      .map(_.flatten)

  private def getPageWidgetsMap(
    ids: Set[PageId]
  ): Future[Map[String, Map[String, WidgetContent]]]                                  =
    ctx
      .run(
        pageWidgetSchema.filter(r => liftQuery(ids).contains(r.pageId))
      )
      .map(_.groupBy(_.pageId).map { case k -> v => k -> v.map(c => c.widgetContentId -> c.toWidgetContent).toMap })

  def canAccessToPage(id: PageId, principals: Set[AnnettePrincipal]): Future[Boolean] =
    for {
      maybeCount <- ctx
                      .run(
                        pageTargetSchema
                          .filter(b =>
                            b.pageId == lift(id) &&
                              liftQuery(principals).contains(b.principal)
                          )
                          .size
                      )
    } yield maybeCount.map(_ > 0).getOrElse(false)

  def getPageViewsById(payload: GetPageViewsPayload): Future[Seq[PageView]] =
    for {
      allowedPageIds    <- getAllowedPageIds(payload.ids, payload.principals + payload.directPrincipal)
      pageViews         <- getPageViewsWithContent(allowedPageIds)
      publishedPageViews = pageViews.filter(page =>
                             page.publicationStatus == PublicationStatus.Published &&
                               page.publicationTimestamp.map(_.compareTo(OffsetDateTime.now) <= 0).getOrElse(true)
                           )
      metrics           <- getPageMetricsById(publishedPageViews.map(_.id).toSet, payload.directPrincipal)
      metricsMap         = metrics.map(a => a.id -> a).toMap

    } yield publishedPageViews
      .map(pv => pv.copy(metric = metricsMap.get(pv.id)))

  def getPageViewsWithContent(ids: Set[String]): Future[Seq[PageView]] =
    for {
      pages          <- ctx.run(
                          pageSchema
                            .filter(b => liftQuery(ids).contains(b.id))
                        )
      pageWidgetsMap <- getPageWidgetsMap(ids)
    } yield pages.map { r =>
      val pageWidgets = pageWidgetsMap.get(r.id).getOrElse(Map.empty)
      r.toPageView(pageWidgets)
    }

  private def getAllowedPageIds(ids: Set[PageId], principals: Set[AnnettePrincipal]): Future[Set[String]] =
    ctx
      .run(
        pageTargetSchema
          .filter(b =>
            liftQuery(ids).contains(b.pageId) &&
              liftQuery(principals).contains(b.principal)
          )
          .map(_.pageId)
      )
      .map(_.toSet)

  // ***************************** metrics update *****************************

  def viewPage(id: PageId, principal: AnnettePrincipal): Future[Done] =
    session
      .executeWrite(
        """UPDATE page_views SET views = views + 1
   WHERE page_id = ? AND principal = ? """,
        id,
        principal.code
      )
      .map(_ => Done)

  def likePage(id: PageId, principal: AnnettePrincipal): Future[Done] =
    ctx.run(
      pageLikeSchema.insert(
        lift(
          PageLikeRecord(
            pageId = id,
            principal = principal
          )
        )
      )
    )

  def unlikePage(id: PageId, principal: AnnettePrincipal): Future[Done] =
    ctx.run(pageLikeSchema.filter(r => r.pageId == lift(id) && r.principal == lift(principal)).delete)

  // ***************************** metrics *****************************

  def getPageMetricsById(ids: Set[PageId], principal: AnnettePrincipal): Future[Seq[PageMetric]] =
    Source(ids)
      .mapAsync(1)(id => getPageMetricById(id, principal))
      .runWith(Sink.seq)

  def getPageMetricById(id: PageId, principal: AnnettePrincipal): Future[PageMetric] =
    for {
      views     <- getPageViewsCountById(id)
      likes     <- getPageLikesCountById(id)
      likedByMe <- getPageLikedByMeById(id, principal)
    } yield PageMetric(id, views, likes, likedByMe)

  private def getPageViewsCountById(id: PageId): Future[Int] =
    for {
      maybeCount <- ctx
                      .run(
                        pageViewSchema
                          .filter(b => b.pageId == lift(id))
                          .size
                      )
    } yield maybeCount.map(_.toInt).getOrElse(0)

  private def getPageLikesCountById(id: PageId): Future[Int] =
    for {
      maybeCount <- ctx
                      .run(
                        pageLikeSchema
                          .filter(b => b.pageId == lift(id))
                          .size
                      )
    } yield maybeCount.map(_.toInt).getOrElse(0)

  private def getPageLikedByMeById(id: PageId, principal: AnnettePrincipal): Future[Boolean] =
    for {
      maybeLike <- ctx
                     .run(
                       pageLikeSchema
                         .filter(b => b.pageId == lift(id) && b.principal == lift(principal))
                         .map(_.pageId)
                     )
                     .map(_.headOption)
    } yield maybeLike.map(_ => true).getOrElse(false)

}
