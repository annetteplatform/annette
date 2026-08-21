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

package biz.lobachev.annette.cms.api

import java.time.OffsetDateTime
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.common._
import biz.lobachev.annette.cms.api.common.article._
import biz.lobachev.annette.cms.api.content.{
  ChangeWidgetOrderPayload,
  Content,
  DeleteWidgetPayload,
  UpdateContentSettingsPayload,
  UpdateWidgetPayload,
  Widget
}
import biz.lobachev.annette.cms.api.files.{FileDescriptor, FileTypes, RemoveFilePayload, RemoveFilesPayload, StoreFilePayload}
import biz.lobachev.annette.cms.api.home_pages._
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.api.{grpc => g}
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.category.{Category, CategoryFindQuery, CategoryId, CreateCategoryPayload, DeleteCategoryPayload, UpdateCategoryPayload}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import org.apache.pekko.Done
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class CmsServiceGrpcImpl(client: g.CmsServiceClient)(implicit val ec: ExecutionContext) extends CmsService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal = AnnettePrincipal(p.code)

  private def parseOdt(s: String): OffsetDateTime = OffsetDateTime.parse(s)

  private def sortByToProto(sortBy: Option[Seq[SortBy]]): Seq[g.SortBy] =
    sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)

  private def toDomain(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  // === content converters ===

  private def widgetToProto(w: Widget): g.Widget =
    g.Widget(id = w.id, widgetType = w.widgetType, data = Json.stringify(w.data), indexData = w.indexData)

  private def widgetFromProto(w: g.Widget): Widget =
    Widget(id = w.id, widgetType = w.widgetType, data = Json.parse(w.data), indexData = w.indexData)

  private def contentToProto(c: Content): g.Content =
    g.Content(
      settings = Json.stringify(c.settings),
      widgetOrder = c.widgetOrder,
      widgets = c.widgets.map { case (k, w) => k -> widgetToProto(w) }
    )

  private def contentFromProto(c: g.Content): Content =
    Content(
      settings = Json.parse(c.settings),
      widgetOrder = c.widgetOrder,
      widgets = c.widgets.map { case (k, w) => k -> widgetFromProto(w) }
    )

  private def updatedFromProto(u: g.Updated): Updated =
    Updated(updatedBy = unwrapPrincipal(u.updatedBy), updatedAt = OffsetDateTime.parse(u.updatedAt))

  private def metricFromProto(m: g.Metric): Metric =
    Metric(id = m.id, views = m.views, likes = m.likes, likedByMe = m.likedByMe)

  private def categoryFromProto(c: g.Category): Category =
    biz.lobachev.annette.core.model.category.Category(
      id = c.id,
      name = c.name,
      updatedAt = OffsetDateTime.parse(c.updatedAt),
      updatedBy = unwrapPrincipal(c.updatedBy)
    )

  private def fileDescriptorFromProto(f: g.FileDescriptor): FileDescriptor =
    FileDescriptor(
      objectId = f.objectId,
      fileType = FileTypes.withName(f.fileType),
      fileId = f.fileId,
      filename = f.filename,
      contentType = f.contentType,
      updatedBy = unwrapPrincipal(f.updatedBy),
      updatedAt = OffsetDateTime.parse(f.updatedAt)
    )

  private def principalsToProto(principals: Set[AnnettePrincipal]): Seq[g.AnnettePrincipal] =
    principals.map(fromDomain).toSeq

  // ************************** CMS Files **************************

  override def storeFile(payload: StoreFilePayload): Future[Done] =
    call(
      client.storeFile(
        g.StoreFilePayload(
          objectId = payload.objectId,
          fileType = payload.fileType.toString,
          fileId = payload.fileId,
          filename = payload.filename,
          contentType = payload.contentType,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def removeFile(payload: RemoveFilePayload): Future[Done] =
    call(
      client.removeFile(
        g.RemoveFilePayload(
          objectId = payload.objectId,
          fileType = payload.fileType.toString,
          fileId = payload.fileId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def removeFiles(payload: RemoveFilesPayload): Future[Done] =
    call(
      client.removeFiles(
        g.RemoveFilesPayload(objectId = payload.objectId, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def getFiles(objectId: String): Future[Seq[FileDescriptor]] =
    call(client.getFiles(g.GetFilesRequest(objectId = objectId)))
      .map(_.files.map(fileDescriptorFromProto))

  // ************************** CMS Blog Categories **************************

  override def createBlogCategory(payload: CreateCategoryPayload): Future[Done] =
    call(
      client.createBlogCategory(
        g.CreateCategoryPayload(id = payload.id, name = payload.name, createdBy = Some(fromDomain(payload.createdBy)))
      )
    ).map(_ => Done)

  override def updateBlogCategory(payload: UpdateCategoryPayload): Future[Done] =
    call(
      client.updateBlogCategory(
        g.UpdateCategoryPayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deleteBlogCategory(payload: DeleteCategoryPayload): Future[Done] =
    call(
      client.deleteBlogCategory(
        g.DeleteCategoryPayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  override def getBlogCategory(id: CategoryId, source: Option[String]): Future[Category] =
    call(client.getBlogCategory(g.GetCategoryRequest(id = id, source = source))).map(categoryFromProto)

  override def getBlogCategories(ids: Set[CategoryId], source: Option[String]): Future[Seq[Category]] =
    call(client.getBlogCategories(g.GetCategoriesRequest(ids = ids.toSeq, source = source)))
      .map(_.categories.map(categoryFromProto))

  override def findBlogCategories(payload: CategoryFindQuery): Future[FindResult] =
    call(
      client.findBlogCategories(
        g.CategoryFindQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          name = payload.name,
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // ************************** CMS Blogs **************************

  private def blogFromProto(b: g.Blog): Blog =
    Blog(
      id = b.id,
      name = b.name,
      description = b.description,
      categoryId = b.categoryId,
      authors = b.authors.map(toDomain).toSet,
      targets = b.targets.map(toDomain).toSet,
      active = b.active,
      updatedBy = unwrapPrincipal(b.updatedBy),
      updatedAt = OffsetDateTime.parse(b.updatedAt)
    )

  private def blogViewFromProto(v: g.BlogView): BlogView =
    BlogView(
      id = v.id,
      name = v.name,
      description = v.description,
      categoryId = v.categoryId,
      active = v.active,
      authors = v.authors.map(toDomain).toSet,
      updatedBy = unwrapPrincipal(v.updatedBy),
      updatedAt = OffsetDateTime.parse(v.updatedAt)
    )

  override def createBlog(payload: CreateBlogPayload): Future[Done] =
    call(
      client.createBlog(
        g.CreateBlogPayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          categoryId = payload.categoryId,
          authors = principalsToProto(payload.authors),
          targets = principalsToProto(payload.targets),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updateBlogName(payload: UpdateNamePayload): Future[Done] =
    call(
      client.updateBlogName(
        g.UpdateNamePayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def updateBlogDescription(payload: UpdateDescriptionPayload): Future[Done] =
    call(
      client.updateBlogDescription(
        g.UpdateDescriptionPayload(
          id = payload.id,
          description = payload.description,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def updateBlogCategoryId(payload: UpdateCategoryIdPayload): Future[Done] =
    call(
      client.updateBlogCategoryId(
        g.UpdateCategoryIdPayload(
          id = payload.id,
          categoryId = payload.categoryId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def assignBlogAuthorPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    call(
      client.assignBlogAuthorPrincipal(
        g.AssignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignBlogAuthorPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    call(
      client.unassignBlogAuthorPrincipal(
        g.UnassignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def assignBlogTargetPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    call(
      client.assignBlogTargetPrincipal(
        g.AssignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignBlogTargetPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    call(
      client.unassignBlogTargetPrincipal(
        g.UnassignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def activateBlog(payload: ActivatePayload): Future[Done] =
    call(
      client.activateBlog(g.ActivatePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def deactivateBlog(payload: DeactivatePayload): Future[Done] =
    call(
      client.deactivateBlog(g.DeactivatePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def deleteBlog(payload: DeletePayload): Future[Done] =
    call(client.deleteBlog(g.DeletePayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))))
      .map(_ => Done)

  override def getBlog(id: BlogId, source: Option[String]): Future[Blog] =
    call(client.getBlog(g.GetBlogRequest(id = id, source = source))).map(blogFromProto)

  override def getBlogs(ids: Set[BlogId], source: Option[String]): Future[Seq[Blog]] =
    call(client.getBlogs(g.GetBlogsRequest(ids = ids.toSeq, source = source)))
      .map(_.blogs.map(blogFromProto))

  override def getBlogViews(payload: GetBlogViewsPayload): Future[Seq[BlogView]] =
    call(
      client.getBlogViews(
        g.GetBlogViewsPayload(ids = payload.ids.toSeq, principals = principalsToProto(payload.principals))
      )
    ).map(_.blogViews.map(blogViewFromProto))

  override def canEditBlogPosts(payload: CanAccessToEntityPayload): Future[Boolean] =
    call(
      client.canEditBlogPosts(
        g.CanAccessToEntityPayload(id = payload.id, principals = principalsToProto(payload.principals))
      )
    ).map(_.value)

  override def canAccessToBlog(payload: CanAccessToEntityPayload): Future[Boolean] =
    call(
      client.canAccessToBlog(
        g.CanAccessToEntityPayload(id = payload.id, principals = principalsToProto(payload.principals))
      )
    ).map(_.value)

  override def findBlogs(payload: BlogFindQuery): Future[FindResult] =
    call(
      client.findBlogs(
        g.BlogFindQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          blogIds = payload.blogIds.map(_.toSeq).getOrElse(Seq.empty),
          categories = payload.categories.map(_.toSeq).getOrElse(Seq.empty),
          authors = payload.authors.map(principalsToProto).getOrElse(Seq.empty),
          targets = payload.targets.map(principalsToProto).getOrElse(Seq.empty),
          active = payload.active,
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // ************************** CMS Posts **************************

  private def postFromProto(p: g.Post): Post =
    Post(
      id = p.id,
      blogId = p.blogId,
      featured = p.featured,
      authorId = unwrapPrincipal(p.authorId),
      title = p.title,
      publicationStatus = PublicationStatus.withName(p.publicationStatus),
      publicationTimestamp = p.publicationTimestamp.map(parseOdt),
      introContent = p.introContent.map(contentFromProto),
      content = p.content.map(contentFromProto),
      targets = if (p.targets.isEmpty) None else Some(p.targets.map(toDomain).toSet),
      metric = p.metric.map(metricFromProto),
      updatedBy = unwrapPrincipal(p.updatedBy),
      updatedAt = OffsetDateTime.parse(p.updatedAt)
    )

  override def createPost(payload: CreatePostPayload): Future[Post] =
    call(
      client.createPost(
        g.CreatePostPayload(
          id = payload.id,
          blogId = payload.blogId,
          featured = payload.featured,
          authorId = Some(fromDomain(payload.authorId)),
          title = payload.title,
          introContent = Some(contentToProto(payload.introContent)),
          content = Some(contentToProto(payload.content)),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(postFromProto)

  override def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Updated] =
    call(
      client.updatePostFeatured(
        g.UpdatePostFeaturedPayload(
          id = payload.id,
          featured = payload.featured,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def updatePostAuthor(payload: UpdateAuthorPayload): Future[Updated] =
    call(
      client.updatePostAuthor(
        g.UpdateAuthorPayload(
          id = payload.id,
          authorId = Some(fromDomain(payload.authorId)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def updatePostTitle(payload: UpdateTitlePayload): Future[Updated] =
    call(
      client.updatePostTitle(
        g.UpdateTitlePayload(id = payload.id, title = payload.title, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(updatedFromProto)

  override def updatePostContentSettings(payload: UpdateContentSettingsPayload): Future[Updated] =
    call(
      client.updatePostContentSettings(
        g.UpdateContentSettingsPayload(
          id = payload.id,
          contentType = payload.contentType.map(_.toString),
          settings = Json.stringify(payload.settings),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def updatePostWidget(payload: UpdateWidgetPayload): Future[Updated] =
    call(
      client.updatePostWidget(
        g.UpdateWidgetPayload(
          id = payload.id,
          contentType = payload.contentType.map(_.toString),
          widget = Some(widgetToProto(payload.widget)),
          order = payload.order,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def changePostWidgetOrder(payload: ChangeWidgetOrderPayload): Future[Updated] =
    call(
      client.changePostWidgetOrder(
        g.ChangeWidgetOrderPayload(
          id = payload.id,
          contentType = payload.contentType.map(_.toString),
          widgetId = payload.widgetId,
          order = payload.order,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def deletePostWidget(payload: DeleteWidgetPayload): Future[Updated] =
    call(
      client.deletePostWidget(
        g.DeleteWidgetPayload(
          id = payload.id,
          contentType = payload.contentType.map(_.toString),
          widgetId = payload.widgetId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def updatePostPublicationTimestamp(payload: UpdatePublicationTimestampPayload): Future[Updated] =
    call(
      client.updatePostPublicationTimestamp(
        g.UpdatePublicationTimestampPayload(
          id = payload.id,
          publicationTimestamp = payload.publicationTimestamp.map(_.toString),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def publishPost(payload: PublishPayload): Future[Updated] =
    call(
      client.publishPost(g.PublishPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(updatedFromProto)

  override def unpublishPost(payload: UnpublishPayload): Future[Updated] =
    call(
      client.unpublishPost(g.UnpublishPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(updatedFromProto)

  override def assignPostTargetPrincipal(payload: AssignPrincipalPayload): Future[Updated] =
    call(
      client.assignPostTargetPrincipal(
        g.AssignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def unassignPostTargetPrincipal(payload: UnassignPrincipalPayload): Future[Updated] =
    call(
      client.unassignPostTargetPrincipal(
        g.UnassignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def deletePost(payload: DeletePayload): Future[Updated] =
    call(client.deletePost(g.DeletePayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))))
      .map(updatedFromProto)

  override def getPost(
    id: PostId,
    source: Option[String],
    withIntro: Option[Boolean],
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Post] =
    call(
      client.getPost(
        g.GetPostRequest(
          id = id,
          source = source,
          withIntro = withIntro,
          withContent = withContent,
          withTargets = withTargets
        )
      )
    ).map(postFromProto)

  override def getPosts(
    ids: Set[PostId],
    source: Option[String],
    withIntro: Option[Boolean],
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Seq[Post]] =
    call(
      client.getPosts(
        g.GetPostsRequest(
          ids = ids.toSeq,
          source = source,
          withIntro = withIntro,
          withContent = withContent,
          withTargets = withTargets
        )
      )
    ).map(_.posts.map(postFromProto))

  override def getPostViews(payload: GetPostViewsPayload): Future[Seq[Post]] =
    call(
      client.getPostViews(
        g.GetPostViewsPayload(
          ids = payload.ids.toSeq,
          directPrincipal = Some(fromDomain(payload.directPrincipal)),
          principals = principalsToProto(payload.principals),
          withContent = payload.withContent
        )
      )
    ).map(_.posts.map(postFromProto))
  override def canEditPost(payload: CanAccessToEntityPayload): Future[Boolean] =
    call(
      client.canEditPost(g.CanAccessToEntityPayload(id = payload.id, principals = principalsToProto(payload.principals)))
    ).map(_.value)

  override def canAccessToPost(payload: CanAccessToEntityPayload): Future[Boolean] =
    call(
      client.canAccessToPost(
        g.CanAccessToEntityPayload(id = payload.id, principals = principalsToProto(payload.principals))
      )
    ).map(_.value)

  override def findPosts(query: PostFindQuery): Future[FindResult] =
    call(
      client.findPosts(
        g.PostFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          postIds = query.postIds.map(_.toSeq).getOrElse(Seq.empty),
          blogs = query.blogs.map(_.toSeq).getOrElse(Seq.empty),
          featured = query.featured,
          authors = query.authors.map(principalsToProto).getOrElse(Seq.empty),
          publicationStatus = query.publicationStatus.map(_.toString),
          publicationTimestampFrom = query.publicationTimestampFrom.map(_.toString),
          publicationTimestampTo = query.publicationTimestampTo.map(_.toString),
          targets = query.targets.map(principalsToProto).getOrElse(Seq.empty),
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(toDomain)

  // ************************** CMS Post Metrics **************************

  override def viewPost(payload: article.ViewPayload): Future[Done] =
    call(client.viewPost(g.ViewPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))))
      .map(_ => Done)

  override def likePost(payload: LikePayload): Future[Done] =
    call(client.likePost(g.LikePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))))
      .map(_ => Done)

  override def unlikePost(payload: UnlikePayload): Future[Done] =
    call(client.unlikePost(g.UnlikePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))))
      .map(_ => Done)

  override def getPostMetric(payload: GetMetricPayload): Future[Metric] =
    call(
      client.getPostMetric(
        g.GetMetricPayload(id = payload.id, principal = Some(fromDomain(payload.principal)))
      )
    ).map(metricFromProto)

  override def getPostMetrics(payload: GetMetricsPayload): Future[Seq[Metric]] =
    call(
      client.getPostMetrics(
        g.GetMetricsPayload(ids = payload.ids, principal = Some(fromDomain(payload.principal)))
      )
    ).map(_.metrics.map(metricFromProto))

  // ************************** CMS Space Categories **************************

  override def createSpaceCategory(payload: CreateCategoryPayload): Future[Done] =
    call(
      client.createSpaceCategory(
        g.CreateCategoryPayload(id = payload.id, name = payload.name, createdBy = Some(fromDomain(payload.createdBy)))
      )
    ).map(_ => Done)

  override def updateSpaceCategory(payload: UpdateCategoryPayload): Future[Done] =
    call(
      client.updateSpaceCategory(
        g.UpdateCategoryPayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deleteSpaceCategory(payload: DeleteCategoryPayload): Future[Done] =
    call(
      client.deleteSpaceCategory(
        g.DeleteCategoryPayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  override def getSpaceCategory(id: CategoryId, source: Option[String]): Future[Category] =
    call(client.getSpaceCategory(g.GetCategoryRequest(id = id, source = source))).map(categoryFromProto)

  override def getSpaceCategories(ids: Set[CategoryId], source: Option[String]): Future[Seq[Category]] =
    call(client.getSpaceCategories(g.GetCategoriesRequest(ids = ids.toSeq, source = source)))
      .map(_.categories.map(categoryFromProto))

  override def findSpaceCategories(payload: CategoryFindQuery): Future[FindResult] =
    call(
      client.findSpaceCategories(
        g.CategoryFindQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          name = payload.name,
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // ************************** CMS Spaces **************************

  private def spaceFromProto(s: g.Space): Space =
    Space(
      id = s.id,
      name = s.name,
      description = s.description,
      categoryId = s.categoryId,
      authors = s.authors.map(toDomain).toSet,
      targets = s.targets.map(toDomain).toSet,
      active = s.active,
      updatedBy = unwrapPrincipal(s.updatedBy),
      updatedAt = OffsetDateTime.parse(s.updatedAt)
    )

  private def spaceViewFromProto(v: g.SpaceView): SpaceView =
    SpaceView(
      id = v.id,
      name = v.name,
      description = v.description,
      categoryId = v.categoryId,
      active = v.active,
      authors = v.authors.map(toDomain).toSet,
      updatedBy = unwrapPrincipal(v.updatedBy),
      updatedAt = OffsetDateTime.parse(v.updatedAt)
    )

  override def createSpace(payload: CreateSpacePayload): Future[Done] =
    call(
      client.createSpace(
        g.CreateSpacePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          categoryId = payload.categoryId,
          authors = principalsToProto(payload.authors),
          targets = principalsToProto(payload.targets),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updateSpaceName(payload: UpdateNamePayload): Future[Done] =
    call(
      client.updateSpaceName(
        g.UpdateNamePayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def updateSpaceDescription(payload: UpdateDescriptionPayload): Future[Done] =
    call(
      client.updateSpaceDescription(
        g.UpdateDescriptionPayload(
          id = payload.id,
          description = payload.description,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def updateSpaceCategoryId(payload: UpdateCategoryIdPayload): Future[Done] =
    call(
      client.updateSpaceCategoryId(
        g.UpdateCategoryIdPayload(
          id = payload.id,
          categoryId = payload.categoryId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def assignSpaceAuthorPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    call(
      client.assignSpaceAuthorPrincipal(
        g.AssignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignSpaceAuthorPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    call(
      client.unassignSpaceAuthorPrincipal(
        g.UnassignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def assignSpaceTargetPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    call(
      client.assignSpaceTargetPrincipal(
        g.AssignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignSpaceTargetPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    call(
      client.unassignSpaceTargetPrincipal(
        g.UnassignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def activateSpace(payload: ActivatePayload): Future[Done] =
    call(
      client.activateSpace(g.ActivatePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def deactivateSpace(payload: DeactivatePayload): Future[Done] =
    call(
      client.deactivateSpace(g.DeactivatePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def deleteSpace(payload: DeletePayload): Future[Done] =
    call(client.deleteSpace(g.DeletePayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))))
      .map(_ => Done)

  override def getSpace(id: SpaceId, source: Option[String]): Future[Space] =
    call(client.getSpace(g.GetSpaceRequest(id = id, source = source))).map(spaceFromProto)

  override def getSpaces(ids: Set[SpaceId], source: Option[String]): Future[Seq[Space]] =
    call(client.getSpaces(g.GetSpacesRequest(ids = ids.toSeq, source = source)))
      .map(_.spaces.map(spaceFromProto))

  override def getSpaceViews(payload: GetSpaceViewsPayload): Future[Seq[SpaceView]] =
    call(
      client.getSpaceViews(
        g.GetSpaceViewsPayload(ids = payload.ids.toSeq, principals = principalsToProto(payload.principals))
      )
    ).map(_.spaceViews.map(spaceViewFromProto))

  override def canEditSpacePages(payload: CanAccessToEntityPayload): Future[Boolean] =
    call(
      client.canEditSpacePages(
        g.CanAccessToEntityPayload(id = payload.id, principals = principalsToProto(payload.principals))
      )
    ).map(_.value)

  override def canAccessToSpace(payload: CanAccessToEntityPayload): Future[Boolean] =
    call(
      client.canAccessToSpace(
        g.CanAccessToEntityPayload(id = payload.id, principals = principalsToProto(payload.principals))
      )
    ).map(_.value)

  override def findSpaces(payload: SpaceFindQuery): Future[FindResult] =
    call(
      client.findSpaces(
        g.SpaceFindQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          spaceIds = payload.spaceIds.map(_.toSeq).getOrElse(Seq.empty),
          categories = payload.categories.map(_.toSeq).getOrElse(Seq.empty),
          authors = payload.authors.map(principalsToProto).getOrElse(Seq.empty),
          targets = payload.targets.map(principalsToProto).getOrElse(Seq.empty),
          active = payload.active,
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // ************************** CMS Pages **************************

  private def pageFromProto(p: g.Page): Page =
    Page(
      id = p.id,
      spaceId = p.spaceId,
      authorId = unwrapPrincipal(p.authorId),
      title = p.title,
      publicationStatus = PublicationStatus.withName(p.publicationStatus),
      publicationTimestamp = p.publicationTimestamp.map(parseOdt),
      content = p.content.map(contentFromProto),
      targets = if (p.targets.isEmpty) None else Some(p.targets.map(toDomain).toSet),
      metric = p.metric.map(metricFromProto),
      updatedBy = unwrapPrincipal(p.updatedBy),
      updatedAt = OffsetDateTime.parse(p.updatedAt)
    )

  override def createPage(payload: CreatePagePayload): Future[Page] =
    call(
      client.createPage(
        g.CreatePagePayload(
          id = payload.id,
          spaceId = payload.spaceId,
          authorId = Some(fromDomain(payload.authorId)),
          title = payload.title,
          content = Some(contentToProto(payload.content)),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(pageFromProto)

  override def updatePageAuthor(payload: UpdateAuthorPayload): Future[Updated] =
    call(
      client.updatePageAuthor(
        g.UpdateAuthorPayload(
          id = payload.id,
          authorId = Some(fromDomain(payload.authorId)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def updatePageTitle(payload: UpdateTitlePayload): Future[Updated] =
    call(
      client.updatePageTitle(
        g.UpdateTitlePayload(id = payload.id, title = payload.title, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(updatedFromProto)

  override def updatePageContentSettings(payload: UpdateContentSettingsPayload): Future[Updated] =
    call(
      client.updatePageContentSettings(
        g.UpdateContentSettingsPayload(
          id = payload.id,
          contentType = payload.contentType.map(_.toString),
          settings = Json.stringify(payload.settings),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def updatePageWidget(payload: UpdateWidgetPayload): Future[Updated] =
    call(
      client.updatePageWidget(
        g.UpdateWidgetPayload(
          id = payload.id,
          contentType = payload.contentType.map(_.toString),
          widget = Some(widgetToProto(payload.widget)),
          order = payload.order,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def changePageWidgetOrder(payload: ChangeWidgetOrderPayload): Future[Updated] =
    call(
      client.changePageWidgetOrder(
        g.ChangeWidgetOrderPayload(
          id = payload.id,
          contentType = payload.contentType.map(_.toString),
          widgetId = payload.widgetId,
          order = payload.order,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def deletePageWidget(payload: DeleteWidgetPayload): Future[Updated] =
    call(
      client.deletePageWidget(
        g.DeleteWidgetPayload(
          id = payload.id,
          contentType = payload.contentType.map(_.toString),
          widgetId = payload.widgetId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def updatePagePublicationTimestamp(payload: UpdatePublicationTimestampPayload): Future[Updated] =
    call(
      client.updatePagePublicationTimestamp(
        g.UpdatePublicationTimestampPayload(
          id = payload.id,
          publicationTimestamp = payload.publicationTimestamp.map(_.toString),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def publishPage(payload: PublishPayload): Future[Updated] =
    call(
      client.publishPage(g.PublishPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(updatedFromProto)

  override def unpublishPage(payload: UnpublishPayload): Future[Updated] =
    call(
      client.unpublishPage(g.UnpublishPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(updatedFromProto)

  override def assignPageTargetPrincipal(payload: AssignPrincipalPayload): Future[Updated] =
    call(
      client.assignPageTargetPrincipal(
        g.AssignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def unassignPageTargetPrincipal(payload: UnassignPrincipalPayload): Future[Updated] =
    call(
      client.unassignPageTargetPrincipal(
        g.UnassignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(updatedFromProto)

  override def deletePage(payload: DeletePayload): Future[Updated] =
    call(client.deletePage(g.DeletePayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))))
      .map(updatedFromProto)

  override def getPage(
    id: PageId,
    source: Option[String],
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Page] =
    call(
      client.getPage(g.GetPageRequest(id = id, source = source, withContent = withContent, withTargets = withTargets))
    ).map(pageFromProto)

  override def getPages(
    ids: Set[PageId],
    source: Option[String],
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Seq[Page]] =
    call(
      client.getPages(
        g.GetPagesRequest(ids = ids.toSeq, source = source, withContent = withContent, withTargets = withTargets)
      )
    ).map(_.pages.map(pageFromProto))

  override def getPageViews(payload: GetPageViewsPayload): Future[Seq[Page]] =
    call(
      client.getPageViews(
        g.GetPageViewsPayload(
          ids = payload.ids.toSeq,
          directPrincipal = Some(fromDomain(payload.directPrincipal)),
          principals = principalsToProto(payload.principals)
        )
      )
    ).map(_.pages.map(pageFromProto))

  override def canEditPage(payload: CanAccessToEntityPayload): Future[Boolean] =
    call(
      client.canEditPage(
        g.CanAccessToEntityPayload(id = payload.id, principals = principalsToProto(payload.principals))
      )
    ).map(_.value)

  override def canAccessToPage(payload: CanAccessToEntityPayload): Future[Boolean] =
    call(
      client.canAccessToPage(
        g.CanAccessToEntityPayload(id = payload.id, principals = principalsToProto(payload.principals))
      )
    ).map(_.value)

  override def findPages(query: PageFindQuery): Future[FindResult] =
    call(
      client.findPages(
        g.PageFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          pageIds = query.pageIds.map(_.toSeq).getOrElse(Seq.empty),
          spaces = query.spaces.map(_.toSeq).getOrElse(Seq.empty),
          authors = query.authors.map(principalsToProto).getOrElse(Seq.empty),
          publicationStatus = query.publicationStatus.map(_.toString),
          publicationTimestampFrom = query.publicationTimestampFrom.map(_.toString),
          publicationTimestampTo = query.publicationTimestampTo.map(_.toString),
          targets = query.targets.map(principalsToProto).getOrElse(Seq.empty),
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(toDomain)

  // ************************** CMS Page Metrics **************************

  override def viewPage(payload: article.ViewPayload): Future[Done] =
    call(client.viewPage(g.ViewPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))))
      .map(_ => Done)

  override def likePage(payload: LikePayload): Future[Done] =
    call(client.likePage(g.LikePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))))
      .map(_ => Done)

  override def unlikePage(payload: UnlikePayload): Future[Done] =
    call(client.unlikePage(g.UnlikePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))))
      .map(_ => Done)

  override def getPageMetric(payload: GetMetricPayload): Future[Metric] =
    call(
      client.getPageMetric(g.GetMetricPayload(id = payload.id, principal = Some(fromDomain(payload.principal))))
    ).map(metricFromProto)

  override def getPageMetrics(payload: GetMetricsPayload): Future[Seq[Metric]] =
    call(
      client.getPageMetrics(g.GetMetricsPayload(ids = payload.ids, principal = Some(fromDomain(payload.principal))))
    ).map(_.metrics.map(metricFromProto))

  // ************************** CMS Home Pages **************************

  private def homePageFromProto(h: g.HomePage): HomePage =
    HomePage(
      id = h.id,
      applicationId = h.applicationId,
      principal = unwrapPrincipal(h.principal),
      priority = h.priority,
      pageId = h.pageId,
      updatedBy = unwrapPrincipal(h.updatedBy),
      updatedAt = OffsetDateTime.parse(h.updatedAt)
    )

  override def assignHomePage(payload: AssignHomePagePayload): Future[Done] =
    call(
      client.assignHomePage(
        g.AssignHomePagePayload(
          applicationId = payload.applicationId,
          principal = Some(fromDomain(payload.principal)),
          priority = payload.priority,
          pageId = payload.pageId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignHomePage(payload: UnassignHomePagePayload): Future[Done] =
    call(
      client.unassignHomePage(
        g.UnassignHomePagePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def getHomePage(id: HomePageId, source: Option[String]): Future[HomePage] =
    call(client.getHomePage(g.GetHomePageRequest(id = id, source = source))).map(homePageFromProto)

  override def getHomePages(ids: Set[HomePageId], source: Option[String]): Future[Seq[HomePage]] =
    call(client.getHomePages(g.GetHomePagesRequest(ids = ids.toSeq, source = source)))
      .map(_.homePages.map(homePageFromProto))

  override def getHomePageByPrincipalCodes(applicationId: String, ids: Seq[String]): Future[PageId] =
    call(
      client.getHomePageByPrincipalCodes(
        g.GetHomePageByPrincipalCodesRequest(applicationId = applicationId, principalCodes = ids)
      )
    ).map(_.pageId)

  override def findHomePages(query: HomePageFindQuery): Future[FindResult] =
    call(
      client.findHomePages(
        g.HomePageFindQuery(
          offset = query.offset,
          size = query.size,
          applicationId = query.applicationId,
          principalCodes = query.principalCodes.map(_.toSeq).getOrElse(Seq.empty),
          principalType = query.principalType,
          principalId = query.principalId,
          pageId = query.pageId,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(toDomain)
}
