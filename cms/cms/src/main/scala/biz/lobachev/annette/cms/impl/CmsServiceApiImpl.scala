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

package biz.lobachev.annette.cms.impl

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.{FindResult, SortBy}
import biz.lobachev.annette.cms.api.content.{
  ChangeWidgetOrderPayload,
  Content,
  ContentTypes,
  DeleteWidgetPayload,
  UpdateContentSettingsPayload,
  UpdateWidgetPayload,
  Widget
}
import biz.lobachev.annette.cms.api.common.article._
import biz.lobachev.annette.cms.api.common._
import biz.lobachev.annette.cms.api.files.{FileDescriptor, FileTypes, RemoveFilePayload, RemoveFilesPayload, StoreFilePayload}
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.home_pages._
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.{grpc => g}
import biz.lobachev.annette.cms.api.grpc.CmsService
import biz.lobachev.annette.cms.impl.blogs.blog.BlogEntityService
import biz.lobachev.annette.cms.impl.blogs.category.BlogCategoryEntityService
import biz.lobachev.annette.cms.impl.blogs.post.PostEntityService
import biz.lobachev.annette.cms.impl.files.FileEntityService
import biz.lobachev.annette.cms.impl.home_pages.HomePageEntityService
import biz.lobachev.annette.cms.impl.pages.category.SpaceCategoryEntityService
import biz.lobachev.annette.cms.impl.pages.page.PageEntityService
import biz.lobachev.annette.cms.impl.pages.space.SpaceEntityService
import biz.lobachev.annette.core.model.DataSource
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class CmsServiceApiImpl(
  blogCategoryEntityService: BlogCategoryEntityService,
  blogEntityService: BlogEntityService,
  postEntityService: PostEntityService,
  spaceCategoryEntityService: SpaceCategoryEntityService,
  spaceEntityService: SpaceEntityService,
  pageEntityService: PageEntityService,
  homePageEntityService: HomePageEntityService,
  fileEntityService: FileEntityService
)(implicit
  ec: ExecutionContext
) extends CmsService {

  val log = LoggerFactory.getLogger(this.getClass)

  // === proto <-> domain converters ===

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal =
    AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def formatOdt(odt: OffsetDateTime): String = odt.toString

  private def parseOdt(s: String): OffsetDateTime = OffsetDateTime.parse(s)

  private def toDomainSortBy(seq: Seq[g.SortBy]): Option[Seq[SortBy]] =
    if (seq.isEmpty) None else Some(seq.map(s => SortBy(field = s.field, descending = s.descending)))

  private def toDomain(w: g.Widget): Widget =
    Widget(
      id = w.id,
      widgetType = w.widgetType,
      data = Json.parse(w.data),
      indexData = w.indexData
    )

  private def fromDomain(w: Widget): g.Widget =
    g.Widget(
      id = w.id,
      widgetType = w.widgetType,
      data = Json.stringify(w.data),
      indexData = w.indexData
    )

  private def toDomain(c: g.Content): Content =
    Content(
      settings = Json.parse(c.settings),
      widgetOrder = c.widgetOrder,
      widgets = c.widgets.map { case (k, w) => k -> toDomain(w) }
    )

  private def fromDomain(c: Content): g.Content =
    g.Content(
      settings = Json.stringify(c.settings),
      widgetOrder = c.widgetOrder,
      widgets = c.widgets.map { case (k, w) => k -> fromDomain(w) }
    )

  private def fromDomain(u: Updated): g.Updated =
    g.Updated(
      updatedBy = Some(fromDomain(u.updatedBy)),
      updatedAt = formatOdt(u.updatedAt)
    )

  private def fromDomain(c: Category): g.Category =
    g.Category(
      id = c.id,
      name = c.name,
      updatedAt = formatOdt(c.updatedAt),
      updatedBy = Some(fromDomain(c.updatedBy))
    )

  private def fromDomain(m: Metric): g.Metric =
    g.Metric(
      id = m.id,
      views = m.views,
      likes = m.likes,
      likedByMe = m.likedByMe
    )

  private def fromDomain(b: Blog): g.Blog =
    g.Blog(
      id = b.id,
      name = b.name,
      description = b.description,
      categoryId = b.categoryId,
      authors = b.authors.toSeq.map(fromDomain),
      targets = b.targets.toSeq.map(fromDomain),
      active = b.active,
      updatedBy = Some(fromDomain(b.updatedBy)),
      updatedAt = formatOdt(b.updatedAt)
    )

  private def fromDomain(v: BlogView): g.BlogView =
    g.BlogView(
      id = v.id,
      name = v.name,
      description = v.description,
      categoryId = v.categoryId,
      active = v.active,
      authors = v.authors.toSeq.map(fromDomain),
      updatedBy = Some(fromDomain(v.updatedBy)),
      updatedAt = formatOdt(v.updatedAt)
    )

  private def fromDomain(p: Post): g.Post =
    g.Post(
      id = p.id,
      blogId = p.blogId,
      featured = p.featured,
      authorId = Some(fromDomain(p.authorId)),
      title = p.title,
      publicationStatus = p.publicationStatus.toString,
      publicationTimestamp = p.publicationTimestamp.map(formatOdt),
      introContent = p.introContent.map(fromDomain),
      content = p.content.map(fromDomain),
      targets = p.targets.map(_.toSeq.map(fromDomain)).getOrElse(Seq.empty),
      metric = p.metric.map(fromDomain),
      updatedBy = Some(fromDomain(p.updatedBy)),
      updatedAt = formatOdt(p.updatedAt)
    )

  private def fromDomain(s: Space): g.Space =
    g.Space(
      id = s.id,
      name = s.name,
      description = s.description,
      categoryId = s.categoryId,
      authors = s.authors.toSeq.map(fromDomain),
      targets = s.targets.toSeq.map(fromDomain),
      active = s.active,
      updatedBy = Some(fromDomain(s.updatedBy)),
      updatedAt = formatOdt(s.updatedAt)
    )

  private def fromDomain(v: SpaceView): g.SpaceView =
    g.SpaceView(
      id = v.id,
      name = v.name,
      description = v.description,
      categoryId = v.categoryId,
      active = v.active,
      authors = v.authors.toSeq.map(fromDomain),
      updatedBy = Some(fromDomain(v.updatedBy)),
      updatedAt = formatOdt(v.updatedAt)
    )

  private def fromDomain(p: Page): g.Page =
    g.Page(
      id = p.id,
      spaceId = p.spaceId,
      authorId = Some(fromDomain(p.authorId)),
      title = p.title,
      publicationStatus = p.publicationStatus.toString,
      publicationTimestamp = p.publicationTimestamp.map(formatOdt),
      content = p.content.map(fromDomain),
      targets = p.targets.map(_.toSeq.map(fromDomain)).getOrElse(Seq.empty),
      metric = p.metric.map(fromDomain),
      updatedBy = Some(fromDomain(p.updatedBy)),
      updatedAt = formatOdt(p.updatedAt)
    )

  private def fromDomain(h: HomePage): g.HomePage =
    g.HomePage(
      id = h.id,
      applicationId = h.applicationId,
      principal = Some(fromDomain(h.principal)),
      priority = h.priority,
      pageId = h.pageId,
      updatedBy = Some(fromDomain(h.updatedBy)),
      updatedAt = formatOdt(h.updatedAt)
    )

  private def fromDomain(f: FileDescriptor): g.FileDescriptor =
    g.FileDescriptor(
      objectId = f.objectId,
      fileType = f.fileType.toString,
      fileId = f.fileId,
      filename = f.filename,
      contentType = f.contentType,
      updatedBy = Some(fromDomain(f.updatedBy)),
      updatedAt = formatOdt(f.updatedAt)
    )

  private def fromDomain(f: FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = formatOdt(h.updatedAt)))
    )

  // ************************** CMS Files **************************

  override def storeFile(in: g.StoreFilePayload): Future[Empty] =
    fileEntityService
      .storeFile(
        StoreFilePayload(
          objectId = in.objectId,
          fileType = FileTypes.withName(in.fileType),
          fileId = in.fileId,
          filename = in.filename,
          contentType = in.contentType,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def removeFile(in: g.RemoveFilePayload): Future[Empty] =
    fileEntityService
      .removeFile(
        RemoveFilePayload(
          objectId = in.objectId,
          fileType = FileTypes.withName(in.fileType),
          fileId = in.fileId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def removeFiles(in: g.RemoveFilesPayload): Future[Empty] =
    fileEntityService
      .removeFiles(
        RemoveFilesPayload(
          objectId = in.objectId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def getFiles(in: g.GetFilesRequest): Future[g.GetFilesResponse] =
    fileEntityService
      .getFiles(in.objectId)
      .map(files => g.GetFilesResponse(files.map(fromDomain)))

  // ************************** CMS Blog Categories **************************

  override def createBlogCategory(in: g.CreateCategoryPayload): Future[Empty] =
    blogCategoryEntityService
      .createCategory(
        CreateCategoryPayload(
          id = in.id,
          name = in.name,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateBlogCategory(in: g.UpdateCategoryPayload): Future[Empty] =
    blogCategoryEntityService
      .updateCategory(
        UpdateCategoryPayload(
          id = in.id,
          name = in.name,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteBlogCategory(in: g.DeleteCategoryPayload): Future[Empty] =
    blogCategoryEntityService
      .deleteCategory(
        DeleteCategoryPayload(
          id = in.id,
          deletedBy = unwrapPrincipal(in.deletedBy)
        )
      )
      .map(_ => Empty())

  override def getBlogCategory(in: g.GetCategoryRequest): Future[g.Category] =
    blogCategoryEntityService
      .getCategory(in.id, in.source)
      .map(fromDomain)

  override def getBlogCategories(in: g.GetCategoriesRequest): Future[g.GetCategoriesResponse] =
    blogCategoryEntityService
      .getCategories(in.ids.toSet, in.source)
      .map(categories => g.GetCategoriesResponse(categories.map(fromDomain)))

  override def findBlogCategories(in: g.CategoryFindQuery): Future[g.FindResult] =
    blogCategoryEntityService
      .findCategories(
        CategoryFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          sortBy = toDomainSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // ************************** CMS Blogs **************************

  override def createBlog(in: g.CreateBlogPayload): Future[Empty] =
    blogEntityService
      .createBlog(
        CreateBlogPayload(
          id = in.id,
          name = in.name,
          description = in.description,
          categoryId = in.categoryId,
          authors = in.authors.map(toDomain).toSet,
          targets = in.targets.map(toDomain).toSet,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateBlogName(in: g.UpdateNamePayload): Future[Empty] =
    blogEntityService
      .updateBlogName(
        UpdateNamePayload(
          id = in.id,
          name = in.name,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def updateBlogDescription(in: g.UpdateDescriptionPayload): Future[Empty] =
    blogEntityService
      .updateBlogDescription(
        UpdateDescriptionPayload(
          id = in.id,
          description = in.description,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def updateBlogCategoryId(in: g.UpdateCategoryIdPayload): Future[Empty] =
    blogEntityService
      .updateBlogCategoryId(
        UpdateCategoryIdPayload(
          id = in.id,
          categoryId = in.categoryId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def assignBlogAuthorPrincipal(in: g.AssignPrincipalPayload): Future[Empty] =
    blogEntityService
      .assignBlogAuthorPrincipal(
        AssignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignBlogAuthorPrincipal(in: g.UnassignPrincipalPayload): Future[Empty] =
    blogEntityService
      .unassignBlogAuthorPrincipal(
        UnassignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def assignBlogTargetPrincipal(in: g.AssignPrincipalPayload): Future[Empty] =
    blogEntityService
      .assignBlogTargetPrincipal(
        AssignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignBlogTargetPrincipal(in: g.UnassignPrincipalPayload): Future[Empty] =
    blogEntityService
      .unassignBlogTargetPrincipal(
        UnassignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def activateBlog(in: g.ActivatePayload): Future[Empty] =
    blogEntityService
      .activateBlog(
        ActivatePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deactivateBlog(in: g.DeactivatePayload): Future[Empty] =
    blogEntityService
      .deactivateBlog(
        DeactivatePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteBlog(in: g.DeletePayload): Future[Empty] =
    blogEntityService
      .deleteBlog(
        DeletePayload(
          id = in.id,
          deletedBy = unwrapPrincipal(in.deletedBy)
        )
      )
      .map(_ => Empty())

  override def getBlog(in: g.GetBlogRequest): Future[g.Blog] =
    blogEntityService
      .getBlog(in.id, in.source)
      .map(fromDomain)

  override def getBlogs(in: g.GetBlogsRequest): Future[g.GetBlogsResponse] =
    blogEntityService
      .getBlogs(in.ids.toSet, in.source)
      .map(blogs => g.GetBlogsResponse(blogs.map(fromDomain)))

  override def getBlogViews(in: g.GetBlogViewsPayload): Future[g.GetBlogViewsResponse] =
    blogEntityService
      .getBlogViews(
        GetBlogViewsPayload(
          ids = in.ids.toSet,
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(views => g.GetBlogViewsResponse(views.map(fromDomain)))

  override def canEditBlogPosts(in: g.CanAccessToEntityPayload): Future[g.BooleanResponse] =
    blogEntityService
      .canEditBlogPosts(
        CanAccessToEntityPayload(
          id = in.id,
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(g.BooleanResponse(_))

  override def canAccessToBlog(in: g.CanAccessToEntityPayload): Future[g.BooleanResponse] =
    blogEntityService
      .canAccessToBlog(
        CanAccessToEntityPayload(
          id = in.id,
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(g.BooleanResponse(_))

  override def findBlogs(in: g.BlogFindQuery): Future[g.FindResult] =
    blogEntityService
      .findBlogs(
        BlogFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          blogIds = if (in.blogIds.isEmpty) None else Some(in.blogIds.toSet),
          categories = if (in.categories.isEmpty) None else Some(in.categories.toSet),
          authors = if (in.authors.isEmpty) None else Some(in.authors.map(toDomain).toSet),
          targets = if (in.targets.isEmpty) None else Some(in.targets.map(toDomain).toSet),
          active = in.active,
          sortBy = toDomainSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // ************************** CMS Posts **************************

  override def createPost(in: g.CreatePostPayload): Future[g.Post] =
    for {
      // validate if blog exist
      // TODO: create isBlogExist method
      blog <- blogEntityService.getBlog(in.blogId, DataSource.FROM_ORIGIN)
      post <- postEntityService
                .createPost(
                  CreatePostPayload(
                    id = in.id,
                    blogId = in.blogId,
                    featured = in.featured,
                    authorId = unwrapPrincipal(in.authorId),
                    title = in.title,
                    introContent = toDomain(in.introContent.getOrElse(throw new IllegalArgumentException("missing intro_content"))),
                    content = toDomain(in.content.getOrElse(throw new IllegalArgumentException("missing content"))),
                    createdBy = unwrapPrincipal(in.createdBy)
                  ),
                  blog.targets
                )
    } yield fromDomain(post)

  override def updatePostFeatured(in: g.UpdatePostFeaturedPayload): Future[g.Updated] =
    postEntityService
      .updatePostFeatured(
        UpdatePostFeaturedPayload(
          id = in.id,
          featured = in.featured,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePostAuthor(in: g.UpdateAuthorPayload): Future[g.Updated] =
    postEntityService
      .updatePostAuthor(
        UpdateAuthorPayload(
          id = in.id,
          authorId = unwrapPrincipal(in.authorId),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePostTitle(in: g.UpdateTitlePayload): Future[g.Updated] =
    postEntityService
      .updatePostTitle(
        UpdateTitlePayload(
          id = in.id,
          title = in.title,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePostContentSettings(in: g.UpdateContentSettingsPayload): Future[g.Updated] =
    postEntityService
      .updatePostContentSettings(
        UpdateContentSettingsPayload(
          id = in.id,
          contentType = in.contentType.map(ContentTypes.withName),
          settings = Json.parse(in.settings),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePostWidget(in: g.UpdateWidgetPayload): Future[g.Updated] =
    postEntityService
      .updateWidget(
        UpdateWidgetPayload(
          id = in.id,
          contentType = in.contentType.map(ContentTypes.withName),
          widget = toDomain(in.widget.getOrElse(throw new IllegalArgumentException("missing widget"))),
          order = in.order,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def changePostWidgetOrder(in: g.ChangeWidgetOrderPayload): Future[g.Updated] =
    postEntityService
      .changeWidgetOrder(
        ChangeWidgetOrderPayload(
          id = in.id,
          contentType = in.contentType.map(ContentTypes.withName),
          widgetId = in.widgetId,
          order = in.order,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def deletePostWidget(in: g.DeleteWidgetPayload): Future[g.Updated] =
    postEntityService
      .deleteWidget(
        DeleteWidgetPayload(
          id = in.id,
          contentType = in.contentType.map(ContentTypes.withName),
          widgetId = in.widgetId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePostPublicationTimestamp(in: g.UpdatePublicationTimestampPayload): Future[g.Updated] =
    postEntityService
      .updatePostPublicationTimestamp(
        UpdatePublicationTimestampPayload(
          id = in.id,
          publicationTimestamp = in.publicationTimestamp.map(parseOdt),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def publishPost(in: g.PublishPayload): Future[g.Updated] =
    postEntityService
      .publishPost(
        PublishPayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def unpublishPost(in: g.UnpublishPayload): Future[g.Updated] =
    postEntityService
      .unpublishPost(
        UnpublishPayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def assignPostTargetPrincipal(in: g.AssignPrincipalPayload): Future[g.Updated] =
    postEntityService
      .assignPostTargetPrincipal(
        AssignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def unassignPostTargetPrincipal(in: g.UnassignPrincipalPayload): Future[g.Updated] =
    postEntityService
      .unassignPostTargetPrincipal(
        UnassignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def deletePost(in: g.DeletePayload): Future[g.Updated] =
    for {
      updated <- postEntityService
                   .deletePost(
                     DeletePayload(
                       id = in.id,
                       deletedBy = unwrapPrincipal(in.deletedBy)
                     )
                   )
      _       <- fileEntityService.removeFiles(
                   RemoveFilesPayload(
                     objectId = s"post-${in.id}",
                     updatedBy = unwrapPrincipal(in.deletedBy)
                   )
                 )
    } yield fromDomain(updated)

  override def getPost(in: g.GetPostRequest): Future[g.Post] =
    postEntityService
      .getPost(
        in.id,
        in.source,
        in.withIntro.getOrElse(false),
        in.withContent.getOrElse(false),
        in.withTargets.getOrElse(false)
      )
      .map(fromDomain)

  override def getPosts(in: g.GetPostsRequest): Future[g.GetPostsResponse] =
    postEntityService
      .getPosts(
        in.ids.toSet,
        in.source,
        in.withIntro.getOrElse(false),
        in.withContent.getOrElse(false),
        in.withTargets.getOrElse(false)
      )
      .map(posts => g.GetPostsResponse(posts.map(fromDomain)))

  override def getPostViews(in: g.GetPostViewsPayload): Future[g.GetPostViewsResponse] =
    postEntityService
      .getPostViews(
        GetPostViewsPayload(
          ids = in.ids.toSet,
          directPrincipal = unwrapPrincipal(in.directPrincipal),
          principals = in.principals.map(toDomain).toSet,
          withContent = in.withContent
        )
      )
      .map(posts => g.GetPostViewsResponse(posts.map(fromDomain)))

  override def canEditPost(in: g.CanAccessToEntityPayload): Future[g.BooleanResponse] =
    for {
      post   <- postEntityService.getPost(in.id, DataSource.FROM_ORIGIN, false, false, false)
      result <- blogEntityService.canEditBlogPosts(
                  CanAccessToEntityPayload(id = post.blogId, principals = in.principals.map(toDomain).toSet)
                )
    } yield g.BooleanResponse(result)

  override def canAccessToPost(in: g.CanAccessToEntityPayload): Future[g.BooleanResponse] =
    postEntityService
      .canAccessToPost(
        CanAccessToEntityPayload(
          id = in.id,
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(g.BooleanResponse(_))

  override def findPosts(in: g.PostFindQuery): Future[g.FindResult] =
    postEntityService
      .findPosts(
        PostFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          postIds = if (in.postIds.isEmpty) None else Some(in.postIds.toSet),
          blogs = if (in.blogs.isEmpty) None else Some(in.blogs.toSet),
          featured = in.featured,
          authors = if (in.authors.isEmpty) None else Some(in.authors.map(toDomain).toSet),
          publicationStatus = in.publicationStatus.map(PublicationStatus.withName),
          publicationTimestampFrom = in.publicationTimestampFrom.map(parseOdt),
          publicationTimestampTo = in.publicationTimestampTo.map(parseOdt),
          targets = if (in.targets.isEmpty) None else Some(in.targets.map(toDomain).toSet),
          sortBy = toDomainSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // ************************** CMS Post Metrics **************************

  override def viewPost(in: g.ViewPayload): Future[Empty] =
    postEntityService
      .viewPost(
        ViewPayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def likePost(in: g.LikePayload): Future[Empty] =
    postEntityService
      .likePost(
        LikePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unlikePost(in: g.UnlikePayload): Future[Empty] =
    postEntityService
      .unlikePost(
        UnlikePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def getPostMetric(in: g.GetMetricPayload): Future[g.Metric] =
    postEntityService
      .getPostMetric(
        GetMetricPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal)
        )
      )
      .map(fromDomain)

  override def getPostMetrics(in: g.GetMetricsPayload): Future[g.GetMetricsResponse] =
    postEntityService
      .getPostMetrics(
        GetMetricsPayload(
          ids = in.ids,
          principal = unwrapPrincipal(in.principal)
        )
      )
      .map(metrics => g.GetMetricsResponse(metrics.map(fromDomain)))

  // ************************** CMS Space Categories **************************

  override def createSpaceCategory(in: g.CreateCategoryPayload): Future[Empty] =
    spaceCategoryEntityService
      .createCategory(
        CreateCategoryPayload(
          id = in.id,
          name = in.name,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateSpaceCategory(in: g.UpdateCategoryPayload): Future[Empty] =
    spaceCategoryEntityService
      .updateCategory(
        UpdateCategoryPayload(
          id = in.id,
          name = in.name,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteSpaceCategory(in: g.DeleteCategoryPayload): Future[Empty] =
    spaceCategoryEntityService
      .deleteCategory(
        DeleteCategoryPayload(
          id = in.id,
          deletedBy = unwrapPrincipal(in.deletedBy)
        )
      )
      .map(_ => Empty())

  override def getSpaceCategory(in: g.GetCategoryRequest): Future[g.Category] =
    spaceCategoryEntityService
      .getCategory(in.id, in.source)
      .map(fromDomain)

  override def getSpaceCategories(in: g.GetCategoriesRequest): Future[g.GetCategoriesResponse] =
    spaceCategoryEntityService
      .getCategories(in.ids.toSet, in.source)
      .map(categories => g.GetCategoriesResponse(categories.map(fromDomain)))

  override def findSpaceCategories(in: g.CategoryFindQuery): Future[g.FindResult] =
    spaceCategoryEntityService
      .findCategories(
        CategoryFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          sortBy = toDomainSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // ************************** CMS Spaces **************************

  override def createSpace(in: g.CreateSpacePayload): Future[Empty] =
    spaceEntityService
      .createSpace(
        CreateSpacePayload(
          id = in.id,
          name = in.name,
          description = in.description,
          categoryId = in.categoryId,
          authors = in.authors.map(toDomain).toSet,
          targets = in.targets.map(toDomain).toSet,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateSpaceName(in: g.UpdateNamePayload): Future[Empty] =
    spaceEntityService
      .updateSpaceName(
        UpdateNamePayload(
          id = in.id,
          name = in.name,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def updateSpaceDescription(in: g.UpdateDescriptionPayload): Future[Empty] =
    spaceEntityService
      .updateSpaceDescription(
        UpdateDescriptionPayload(
          id = in.id,
          description = in.description,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def updateSpaceCategoryId(in: g.UpdateCategoryIdPayload): Future[Empty] =
    spaceEntityService
      .updateSpaceCategoryId(
        UpdateCategoryIdPayload(
          id = in.id,
          categoryId = in.categoryId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def assignSpaceAuthorPrincipal(in: g.AssignPrincipalPayload): Future[Empty] =
    spaceEntityService
      .assignSpaceAuthorPrincipal(
        AssignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignSpaceAuthorPrincipal(in: g.UnassignPrincipalPayload): Future[Empty] =
    spaceEntityService
      .unassignSpaceAuthorPrincipal(
        UnassignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def assignSpaceTargetPrincipal(in: g.AssignPrincipalPayload): Future[Empty] =
    spaceEntityService
      .assignSpaceTargetPrincipal(
        AssignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignSpaceTargetPrincipal(in: g.UnassignPrincipalPayload): Future[Empty] =
    spaceEntityService
      .unassignSpaceTargetPrincipal(
        UnassignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def activateSpace(in: g.ActivatePayload): Future[Empty] =
    spaceEntityService
      .activateSpace(
        ActivatePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deactivateSpace(in: g.DeactivatePayload): Future[Empty] =
    spaceEntityService
      .deactivateSpace(
        DeactivatePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteSpace(in: g.DeletePayload): Future[Empty] =
    spaceEntityService
      .deleteSpace(
        DeletePayload(
          id = in.id,
          deletedBy = unwrapPrincipal(in.deletedBy)
        )
      )
      .map(_ => Empty())

  override def getSpace(in: g.GetSpaceRequest): Future[g.Space] =
    spaceEntityService
      .getSpace(in.id, in.source)
      .map(fromDomain)

  override def getSpaces(in: g.GetSpacesRequest): Future[g.GetSpacesResponse] =
    spaceEntityService
      .getSpaces(in.ids.toSet, in.source)
      .map(spaces => g.GetSpacesResponse(spaces.map(fromDomain)))

  override def getSpaceViews(in: g.GetSpaceViewsPayload): Future[g.GetSpaceViewsResponse] =
    spaceEntityService
      .getSpaceViews(
        GetSpaceViewsPayload(
          ids = in.ids.toSet,
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(views => g.GetSpaceViewsResponse(views.map(fromDomain)))

  override def canEditSpacePages(in: g.CanAccessToEntityPayload): Future[g.BooleanResponse] =
    spaceEntityService
      .canEditSpacePages(
        CanAccessToEntityPayload(
          id = in.id,
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(g.BooleanResponse(_))

  override def canAccessToSpace(in: g.CanAccessToEntityPayload): Future[g.BooleanResponse] =
    spaceEntityService
      .canAccessToSpace(
        CanAccessToEntityPayload(
          id = in.id,
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(g.BooleanResponse(_))

  override def findSpaces(in: g.SpaceFindQuery): Future[g.FindResult] =
    spaceEntityService
      .findSpaces(
        SpaceFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          spaceIds = if (in.spaceIds.isEmpty) None else Some(in.spaceIds.toSet),
          categories = if (in.categories.isEmpty) None else Some(in.categories.toSet),
          authors = if (in.authors.isEmpty) None else Some(in.authors.map(toDomain).toSet),
          targets = if (in.targets.isEmpty) None else Some(in.targets.map(toDomain).toSet),
          active = in.active,
          sortBy = toDomainSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // ************************** CMS Pages **************************

  override def createPage(in: g.CreatePagePayload): Future[g.Page] =
    for {
      // validate if space exist
      // TODO: create isSpaceExist method
      space <- spaceEntityService.getSpace(in.spaceId, DataSource.FROM_ORIGIN)
      page  <- pageEntityService
                 .createPage(
                   CreatePagePayload(
                     id = in.id,
                     spaceId = in.spaceId,
                     authorId = unwrapPrincipal(in.authorId),
                     title = in.title,
                     content = toDomain(in.content.getOrElse(throw new IllegalArgumentException("missing content"))),
                     createdBy = unwrapPrincipal(in.createdBy)
                   ),
                   space.targets
                 )
    } yield fromDomain(page)

  override def updatePageAuthor(in: g.UpdateAuthorPayload): Future[g.Updated] =
    pageEntityService
      .updatePageAuthor(
        UpdateAuthorPayload(
          id = in.id,
          authorId = unwrapPrincipal(in.authorId),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePageTitle(in: g.UpdateTitlePayload): Future[g.Updated] =
    pageEntityService
      .updatePageTitle(
        UpdateTitlePayload(
          id = in.id,
          title = in.title,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePageContentSettings(in: g.UpdateContentSettingsPayload): Future[g.Updated] =
    pageEntityService
      .updatePageContentSettings(
        UpdateContentSettingsPayload(
          id = in.id,
          contentType = in.contentType.map(ContentTypes.withName),
          settings = Json.parse(in.settings),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePageWidget(in: g.UpdateWidgetPayload): Future[g.Updated] =
    pageEntityService
      .updateWidget(
        UpdateWidgetPayload(
          id = in.id,
          contentType = in.contentType.map(ContentTypes.withName),
          widget = toDomain(in.widget.getOrElse(throw new IllegalArgumentException("missing widget"))),
          order = in.order,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def changePageWidgetOrder(in: g.ChangeWidgetOrderPayload): Future[g.Updated] =
    pageEntityService
      .changeWidgetOrder(
        ChangeWidgetOrderPayload(
          id = in.id,
          contentType = in.contentType.map(ContentTypes.withName),
          widgetId = in.widgetId,
          order = in.order,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def deletePageWidget(in: g.DeleteWidgetPayload): Future[g.Updated] =
    pageEntityService
      .deleteWidget(
        DeleteWidgetPayload(
          id = in.id,
          contentType = in.contentType.map(ContentTypes.withName),
          widgetId = in.widgetId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def updatePagePublicationTimestamp(in: g.UpdatePublicationTimestampPayload): Future[g.Updated] =
    pageEntityService
      .updatePagePublicationTimestamp(
        UpdatePublicationTimestampPayload(
          id = in.id,
          publicationTimestamp = in.publicationTimestamp.map(parseOdt),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def publishPage(in: g.PublishPayload): Future[g.Updated] =
    pageEntityService
      .publishPage(
        PublishPayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def unpublishPage(in: g.UnpublishPayload): Future[g.Updated] =
    pageEntityService
      .unpublishPage(
        UnpublishPayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def assignPageTargetPrincipal(in: g.AssignPrincipalPayload): Future[g.Updated] =
    pageEntityService
      .assignPageTargetPrincipal(
        AssignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def unassignPageTargetPrincipal(in: g.UnassignPrincipalPayload): Future[g.Updated] =
    pageEntityService
      .unassignPageTargetPrincipal(
        UnassignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(fromDomain)

  override def deletePage(in: g.DeletePayload): Future[g.Updated] =
    for {
      updated <- pageEntityService
                   .deletePage(
                     DeletePayload(
                       id = in.id,
                       deletedBy = unwrapPrincipal(in.deletedBy)
                     )
                   )
      _       <- fileEntityService.removeFiles(
                   RemoveFilesPayload(
                     objectId = s"page-${in.id}",
                     updatedBy = unwrapPrincipal(in.deletedBy)
                   )
                 )
    } yield fromDomain(updated)

  override def getPage(in: g.GetPageRequest): Future[g.Page] =
    pageEntityService
      .getPage(
        in.id,
        in.source,
        in.withContent.getOrElse(false),
        in.withTargets.getOrElse(false)
      )
      .map(fromDomain)

  override def getPages(in: g.GetPagesRequest): Future[g.GetPagesResponse] =
    pageEntityService
      .getPages(
        in.ids.toSet,
        in.source,
        in.withContent.getOrElse(false),
        in.withTargets.getOrElse(false)
      )
      .map(pages => g.GetPagesResponse(pages.map(fromDomain)))

  override def getPageViews(in: g.GetPageViewsPayload): Future[g.GetPageViewsResponse] =
    pageEntityService
      .getPageViews(
        GetPageViewsPayload(
          ids = in.ids.toSet,
          directPrincipal = unwrapPrincipal(in.directPrincipal),
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(pages => g.GetPageViewsResponse(pages.map(fromDomain)))

  override def canEditPage(in: g.CanAccessToEntityPayload): Future[g.BooleanResponse] =
    for {
      page   <- pageEntityService.getPage(in.id, DataSource.FROM_ORIGIN, false, false)
      result <- spaceEntityService.canEditSpacePages(
                  CanAccessToEntityPayload(id = page.spaceId, principals = in.principals.map(toDomain).toSet)
                )
    } yield g.BooleanResponse(result)

  override def canAccessToPage(in: g.CanAccessToEntityPayload): Future[g.BooleanResponse] =
    pageEntityService
      .canAccessToPage(
        CanAccessToEntityPayload(
          id = in.id,
          principals = in.principals.map(toDomain).toSet
        )
      )
      .map(g.BooleanResponse(_))

  override def findPages(in: g.PageFindQuery): Future[g.FindResult] =
    pageEntityService
      .findPages(
        PageFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          pageIds = if (in.pageIds.isEmpty) None else Some(in.pageIds.toSet),
          spaces = if (in.spaces.isEmpty) None else Some(in.spaces.toSet),
          authors = if (in.authors.isEmpty) None else Some(in.authors.map(toDomain).toSet),
          publicationStatus = in.publicationStatus.map(PublicationStatus.withName),
          publicationTimestampFrom = in.publicationTimestampFrom.map(parseOdt),
          publicationTimestampTo = in.publicationTimestampTo.map(parseOdt),
          targets = if (in.targets.isEmpty) None else Some(in.targets.map(toDomain).toSet),
          sortBy = toDomainSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // ************************** CMS Page Metrics **************************

  override def viewPage(in: g.ViewPayload): Future[Empty] =
    pageEntityService
      .viewPage(
        ViewPayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def likePage(in: g.LikePayload): Future[Empty] =
    pageEntityService
      .likePage(
        LikePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unlikePage(in: g.UnlikePayload): Future[Empty] =
    pageEntityService
      .unlikePage(
        UnlikePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def getPageMetric(in: g.GetMetricPayload): Future[g.Metric] =
    pageEntityService
      .getPageMetric(
        GetMetricPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal)
        )
      )
      .map(fromDomain)

  override def getPageMetrics(in: g.GetMetricsPayload): Future[g.GetMetricsResponse] =
    pageEntityService
      .getPageMetrics(
        GetMetricsPayload(
          ids = in.ids,
          principal = unwrapPrincipal(in.principal)
        )
      )
      .map(metrics => g.GetMetricsResponse(metrics.map(fromDomain)))

  // ************************** CMS Home Page **************************

  override def assignHomePage(in: g.AssignHomePagePayload): Future[Empty] =
    homePageEntityService
      .assignHomePage(
        AssignHomePagePayload(
          applicationId = in.applicationId,
          principal = unwrapPrincipal(in.principal),
          priority = in.priority,
          pageId = in.pageId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignHomePage(in: g.UnassignHomePagePayload): Future[Empty] =
    homePageEntityService
      .unassignHomePage(
        UnassignHomePagePayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def getHomePage(in: g.GetHomePageRequest): Future[g.HomePage] =
    homePageEntityService
      .getHomePage(in.id, in.source)
      .map(fromDomain)

  override def getHomePages(in: g.GetHomePagesRequest): Future[g.GetHomePagesResponse] =
    homePageEntityService
      .getHomePages(in.ids.toSet, in.source)
      .map(homePages => g.GetHomePagesResponse(homePages.map(fromDomain)))

  override def getHomePageByPrincipalCodes(
    in: g.GetHomePageByPrincipalCodesRequest
  ): Future[g.GetHomePageByPrincipalCodesResponse] =
    homePageEntityService
      .getHomePageByPrincipalCodes(in.applicationId, in.principalCodes)
      .map(pageId => g.GetHomePageByPrincipalCodesResponse(pageId))

  override def findHomePages(in: g.HomePageFindQuery): Future[g.FindResult] =
    homePageEntityService
      .findHomePages(
        HomePageFindQuery(
          offset = in.offset,
          size = in.size,
          applicationId = in.applicationId,
          principalCodes = if (in.principalCodes.isEmpty) None else Some(in.principalCodes.toSet),
          principalType = in.principalType,
          principalId = in.principalId,
          pageId = in.pageId,
          sortBy = toDomainSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

}
