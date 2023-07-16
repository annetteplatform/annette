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

import akka.{Done, NotUsed}
import biz.lobachev.annette.cms.api.{common, _}
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.api.blogs.post._
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
  UpdateTitlePayload
}
import biz.lobachev.annette.cms.api.common.{
  article,
  ActivatePayload,
  AssignPrincipalPayload,
  CanAccessToEntityPayload,
  DeactivatePayload,
  DeletePayload,
  UnassignPrincipalPayload,
  UpdateDescriptionPayload,
  UpdateNamePayload,
  Updated
}
import biz.lobachev.annette.cms.api.content.{
  ChangeWidgetOrderPayload,
  DeleteWidgetPayload,
  UpdateContentSettingsPayload,
  UpdateWidgetPayload
}
import biz.lobachev.annette.cms.api.files._
import biz.lobachev.annette.cms.api.home_pages.{
  AssignHomePagePayload,
  HomePage,
  HomePageFindQuery,
  HomePageId,
  UnassignHomePagePayload
}
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.impl.blogs.blog._
import biz.lobachev.annette.cms.impl.blogs.category.BlogCategoryEntityService
import biz.lobachev.annette.cms.impl.blogs.post._
import biz.lobachev.annette.cms.impl.files.FileEntityService
import biz.lobachev.annette.cms.impl.home_pages.HomePageEntityService
import biz.lobachev.annette.cms.impl.pages.category.SpaceCategoryEntityService
import biz.lobachev.annette.cms.impl.pages.page.PageEntityService
import biz.lobachev.annette.cms.impl.pages.space.SpaceEntityService
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

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
) extends CmsServiceApi {

  val log = LoggerFactory.getLogger(this.getClass)

  // ************************** CMS Files **************************

  override def storeFile: ServiceCall[StoreFilePayload, Done] =
    ServiceCall { payload =>
      fileEntityService.storeFile(payload)
    }

  override def removeFile: ServiceCall[RemoveFilePayload, Done] =
    ServiceCall { payload =>
      fileEntityService.removeFile(payload)
    }

  override def removeFiles: ServiceCall[RemoveFilesPayload, Done] =
    ServiceCall { payload =>
      fileEntityService.removeFiles(payload)
    }

  override def getFiles(objectId: String): ServiceCall[NotUsed, Seq[FileDescriptor]] =
    ServiceCall { _ =>
      fileEntityService.getFiles(objectId)
    }

  // ************************** CMS Blogs **************************

  override def createBlogCategory: ServiceCall[CreateCategoryPayload, Done] =
    ServiceCall { payload =>
      blogCategoryEntityService.createCategory(payload)
    }

  override def updateBlogCategory: ServiceCall[UpdateCategoryPayload, Done] =
    ServiceCall { payload =>
      blogCategoryEntityService.updateCategory(payload)
    }

  override def deleteBlogCategory: ServiceCall[DeleteCategoryPayload, Done] =
    ServiceCall { payload =>
      blogCategoryEntityService.deleteCategory(payload)
    }

  override def getBlogCategory(id: CategoryId, source: Option[String] = None): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      blogCategoryEntityService.getCategory(id, source)
    }

  override def getBlogCategories(
    source: Option[String] = None
  ): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      blogCategoryEntityService.getCategories(ids, source)
    }

  override def findBlogCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      blogCategoryEntityService.findCategories(query)
    }

  override def createBlog: ServiceCall[CreateBlogPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.createBlog(payload)
    }

  override def updateBlogName: ServiceCall[UpdateNamePayload, Done] =
    ServiceCall { payload =>
      blogEntityService.updateBlogName(payload)
    }

  override def updateBlogDescription: ServiceCall[UpdateDescriptionPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.updateBlogDescription(payload)
    }

  override def updateBlogCategoryId: ServiceCall[common.UpdateCategoryIdPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.updateBlogCategoryId(payload)
    }

  override def assignBlogAuthorPrincipal: ServiceCall[AssignPrincipalPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.assignBlogAuthorPrincipal(payload)
    }

  override def unassignBlogAuthorPrincipal: ServiceCall[UnassignPrincipalPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.unassignBlogAuthorPrincipal(payload)
    }

  override def assignBlogTargetPrincipal: ServiceCall[AssignPrincipalPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.assignBlogTargetPrincipal(payload)
    }

  override def unassignBlogTargetPrincipal: ServiceCall[UnassignPrincipalPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.unassignBlogTargetPrincipal(payload)
    }

  override def activateBlog: ServiceCall[ActivatePayload, Done] =
    ServiceCall { payload =>
      blogEntityService.activateBlog(payload)
    }

  override def deactivateBlog: ServiceCall[DeactivatePayload, Done] =
    ServiceCall { payload =>
      blogEntityService.deactivateBlog(payload)
    }

  override def deleteBlog: ServiceCall[DeletePayload, Done] =
    ServiceCall { payload =>
      // TODO: validate if posts exist
      blogEntityService.deleteBlog(payload)
    }

  override def getBlog(id: BlogId, source: Option[String] = None): ServiceCall[NotUsed, Blog] =
    ServiceCall { _ =>
      blogEntityService.getBlog(id, source)
    }

  override def getBlogs(source: Option[String] = None): ServiceCall[Set[BlogId], Seq[Blog]] =
    ServiceCall { ids =>
      blogEntityService.getBlogs(ids, source)
    }

  override def getBlogViews: ServiceCall[GetBlogViewsPayload, Seq[BlogView]] =
    ServiceCall { payload =>
      blogEntityService.getBlogViews(payload)
    }

  override def canEditBlogPosts: ServiceCall[CanAccessToEntityPayload, Boolean] =
    ServiceCall { payload =>
      blogEntityService.canEditBlogPosts(payload)
    }
  override def canAccessToBlog: ServiceCall[CanAccessToEntityPayload, Boolean]  =
    ServiceCall { payload =>
      blogEntityService.canAccessToBlog(payload)
    }

  override def findBlogs: ServiceCall[BlogFindQuery, FindResult] =
    ServiceCall { query =>
      blogEntityService.findBlogs(query)
    }

  override def createPost: ServiceCall[CreatePostPayload, Post] =
    ServiceCall { payload =>
      for {
        // validate if blog exist
        // TODO: create isBlogExist method
        blog    <- blogEntityService.getBlog(payload.blogId, DataSource.FROM_ORIGIN)
        updated <- postEntityService
                     .createPost(payload, blog.targets)

      } yield updated
    }

  override def updatePostFeatured: ServiceCall[UpdatePostFeaturedPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.updatePostFeatured(payload)
    }

  override def updatePostAuthor: ServiceCall[UpdateAuthorPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.updatePostAuthor(payload)
    }

  override def updatePostTitle: ServiceCall[UpdateTitlePayload, Updated]                     =
    ServiceCall { payload =>
      postEntityService.updatePostTitle(payload)
    }
  override def updatePostContentSettings: ServiceCall[UpdateContentSettingsPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.updatePostContentSettings(payload)
    }

  override def updatePostWidget: ServiceCall[UpdateWidgetPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.updateWidget(payload)
    }

  override def changePostWidgetOrder: ServiceCall[ChangeWidgetOrderPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.changeWidgetOrder(payload)
    }

  override def deletePostWidget: ServiceCall[DeleteWidgetPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.deleteWidget(payload)
    }

  override def updatePostPublicationTimestamp: ServiceCall[UpdatePublicationTimestampPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.updatePostPublicationTimestamp(payload)
    }

  override def publishPost: ServiceCall[PublishPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.publishPost(payload)
    }

  override def unpublishPost: ServiceCall[UnpublishPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.unpublishPost(payload)
    }

  override def assignPostTargetPrincipal: ServiceCall[AssignPrincipalPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.assignPostTargetPrincipal(payload)
    }

  override def unassignPostTargetPrincipal: ServiceCall[UnassignPrincipalPayload, Updated] =
    ServiceCall { payload =>
      postEntityService.unassignPostTargetPrincipal(payload)
    }

  override def deletePost: ServiceCall[DeletePayload, Updated] =
    ServiceCall { payload =>
      for {
        updated <- postEntityService.deletePost(payload)
        _       <- fileEntityService.removeFiles(
                     RemoveFilesPayload(
                       objectId = s"post-${payload.id}",
                       updatedBy = payload.deletedBy
                     )
                   )
      } yield updated
    }

  override def getPost(
    id: PostId,
    source: Option[String] = None,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[NotUsed, Post] =
    ServiceCall { _ =>
      postEntityService.getPost(
        id,
        source,
        withIntro.getOrElse(false),
        withContent.getOrElse(false),
        withTargets.getOrElse(false)
      )
    }

  override def getPosts(
    source: Option[String] = None,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[Set[PostId], Seq[Post]] =
    ServiceCall { ids =>
      postEntityService.getPosts(
        ids,
        source,
        withIntro.getOrElse(false),
        withContent.getOrElse(false),
        withTargets.getOrElse(false)
      )
    }

  override def getPostViews: ServiceCall[GetPostViewsPayload, Seq[Post]] =
    ServiceCall { payload =>
      postEntityService.getPostViews(payload)
    }

  override def canEditPost: ServiceCall[CanAccessToEntityPayload, Boolean] =
    ServiceCall { payload =>
      for {
        post   <- postEntityService.getPost(payload.id, DataSource.FROM_ORIGIN, false, false, false)
        result <- blogEntityService.canEditBlogPosts(payload.copy(id = post.blogId))
      } yield result
    }

  override def canAccessToPost: ServiceCall[CanAccessToEntityPayload, Boolean] =
    ServiceCall { payload =>
      postEntityService.canAccessToPost(payload)
    }

  override def findPosts: ServiceCall[PostFindQuery, FindResult] =
    ServiceCall { query =>
      postEntityService.findPosts(query)
    }

  override def viewPost: ServiceCall[article.ViewPayload, Done] =
    ServiceCall { payload =>
      postEntityService.viewPost(payload)
    }

  override def likePost: ServiceCall[LikePayload, Done] =
    ServiceCall { payload =>
      postEntityService.likePost(payload)
    }

  override def unlikePost: ServiceCall[UnlikePayload, Done] =
    ServiceCall { payload =>
      postEntityService.unlikePost(payload)
    }

  override def getPostMetric: ServiceCall[GetMetricPayload, Metric] =
    ServiceCall { payload =>
      postEntityService.getPostMetric(payload)
    }

  override def getPostMetrics: ServiceCall[GetMetricsPayload, Seq[Metric]] =
    ServiceCall { payload =>
      postEntityService.getPostMetrics(payload)
    }

  // ************************** CMS Pages **************************

  override def createSpaceCategory: ServiceCall[CreateCategoryPayload, Done] =
    ServiceCall { payload =>
      spaceCategoryEntityService.createCategory(payload)
    }

  override def updateSpaceCategory: ServiceCall[UpdateCategoryPayload, Done] =
    ServiceCall { payload =>
      spaceCategoryEntityService.updateCategory(payload)
    }

  override def deleteSpaceCategory: ServiceCall[DeleteCategoryPayload, Done] =
    ServiceCall { payload =>
      spaceCategoryEntityService.deleteCategory(payload)
    }

  override def getSpaceCategory(id: CategoryId, source: Option[String] = None): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      spaceCategoryEntityService.getCategory(id, source)
    }

  override def getSpaceCategories(
    source: Option[String] = None
  ): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      spaceCategoryEntityService.getCategories(ids, source)
    }

  override def findSpaceCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      spaceCategoryEntityService.findCategories(query)
    }

  override def createSpace: ServiceCall[CreateSpacePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.createSpace(payload)
    }

  override def updateSpaceName: ServiceCall[UpdateNamePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.updateSpaceName(payload)
    }

  override def updateSpaceDescription: ServiceCall[UpdateDescriptionPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.updateSpaceDescription(payload)
    }

  override def updateSpaceCategoryId: ServiceCall[common.UpdateCategoryIdPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.updateSpaceCategoryId(payload)
    }

  override def assignSpaceAuthorPrincipal: ServiceCall[AssignPrincipalPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.assignSpaceAuthorPrincipal(payload)
    }

  override def unassignSpaceAuthorPrincipal: ServiceCall[UnassignPrincipalPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.unassignSpaceAuthorPrincipal(payload)
    }

  override def assignSpaceTargetPrincipal: ServiceCall[AssignPrincipalPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.assignSpaceTargetPrincipal(payload)
    }

  override def unassignSpaceTargetPrincipal: ServiceCall[UnassignPrincipalPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.unassignSpaceTargetPrincipal(payload)
    }

  override def activateSpace: ServiceCall[ActivatePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.activateSpace(payload)
    }

  override def deactivateSpace: ServiceCall[DeactivatePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.deactivateSpace(payload)
    }

  override def deleteSpace: ServiceCall[DeletePayload, Done] =
    ServiceCall { payload =>
      // TODO: validate if pages exist
      spaceEntityService.deleteSpace(payload)
    }

  override def getSpace(id: SpaceId, source: Option[String] = None): ServiceCall[NotUsed, Space] =
    ServiceCall { _ =>
      spaceEntityService.getSpace(id, source)
    }

  override def getSpaces(source: Option[String] = None): ServiceCall[Set[SpaceId], Seq[Space]] =
    ServiceCall { ids =>
      spaceEntityService.getSpaces(ids, source)
    }

  override def getSpaceViews: ServiceCall[GetSpaceViewsPayload, Seq[SpaceView]] =
    ServiceCall { payload =>
      spaceEntityService.getSpaceViews(payload)
    }

  override def canEditSpacePages: ServiceCall[CanAccessToEntityPayload, Boolean] =
    ServiceCall { payload =>
      spaceEntityService.canEditSpacePages(payload)
    }

  override def canAccessToSpace: ServiceCall[CanAccessToEntityPayload, Boolean] =
    ServiceCall { payload =>
      spaceEntityService.canAccessToSpace(payload)
    }

  override def findSpaces: ServiceCall[SpaceFindQuery, FindResult] =
    ServiceCall { query =>
      spaceEntityService.findSpaces(query)
    }

  override def createPage: ServiceCall[CreatePagePayload, Page] =
    ServiceCall { payload =>
      for {
        // validate if space exist
        // TODO: create isSpaceExist method
        space <- spaceEntityService.getSpace(payload.spaceId, DataSource.FROM_ORIGIN)
        page  <- pageEntityService
                   .createPage(payload, space.targets)

      } yield page
    }

  override def updatePageAuthor: ServiceCall[UpdateAuthorPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.updatePageAuthor(payload)
    }

  override def updatePageTitle: ServiceCall[UpdateTitlePayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.updatePageTitle(payload)
    }

  override def updatePageContentSettings: ServiceCall[UpdateContentSettingsPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.updatePageContentSettings(payload)
    }

  override def updatePageWidget: ServiceCall[UpdateWidgetPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.updateWidget(payload)
    }

  override def changePageWidgetOrder: ServiceCall[ChangeWidgetOrderPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.changeWidgetOrder(payload)
    }

  override def deletePageWidget: ServiceCall[DeleteWidgetPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.deleteWidget(payload)
    }

  override def updatePagePublicationTimestamp: ServiceCall[UpdatePublicationTimestampPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.updatePagePublicationTimestamp(payload)
    }

  override def publishPage: ServiceCall[PublishPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.publishPage(payload)
    }

  override def unpublishPage: ServiceCall[UnpublishPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.unpublishPage(payload)
    }

  override def assignPageTargetPrincipal: ServiceCall[AssignPrincipalPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.assignPageTargetPrincipal(payload)
    }

  override def unassignPageTargetPrincipal: ServiceCall[UnassignPrincipalPayload, Updated] =
    ServiceCall { payload =>
      pageEntityService.unassignPageTargetPrincipal(payload)
    }

  override def deletePage: ServiceCall[DeletePayload, Updated] =
    ServiceCall { payload =>
      for {
        updated <- pageEntityService.deletePage(payload)
        _       <- fileEntityService.removeFiles(
                     RemoveFilesPayload(
                       objectId = s"page-${payload.id}",
                       updatedBy = payload.deletedBy
                     )
                   )
      } yield updated
    }

  override def getPage(
    id: PageId,
    source: Option[String] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[NotUsed, Page] =
    ServiceCall { _ =>
      pageEntityService.getPage(
        id,
        source,
        withContent.getOrElse(false),
        withTargets.getOrElse(false)
      )
    }

  override def getPages(
    source: Option[String] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[Set[PageId], Seq[Page]] =
    ServiceCall { ids =>
      pageEntityService.getPages(
        ids,
        source,
        withContent.getOrElse(false),
        withTargets.getOrElse(false)
      )
    }

  override def getPageViews: ServiceCall[GetPageViewsPayload, Seq[Page]] =
    ServiceCall { payload =>
      pageEntityService.getPageViews(payload)
    }

  override def canEditPage: ServiceCall[CanAccessToEntityPayload, Boolean] =
    ServiceCall { payload =>
      for {
        page   <- pageEntityService.getPage(payload.id, DataSource.FROM_ORIGIN, false, false)
        result <- spaceEntityService.canEditSpacePages(payload.copy(id = page.spaceId))
      } yield result
    }

  override def canAccessToPage: ServiceCall[CanAccessToEntityPayload, Boolean] =
    ServiceCall { payload =>
      pageEntityService.canAccessToPage(payload)
    }

  override def findPages: ServiceCall[PageFindQuery, FindResult] =
    ServiceCall { query =>
      pageEntityService.findPages(query)
    }

  override def viewPage: ServiceCall[article.ViewPayload, Done] =
    ServiceCall { payload =>
      pageEntityService.viewPage(payload)
    }

  override def likePage: ServiceCall[LikePayload, Done] =
    ServiceCall { payload =>
      pageEntityService.likePage(payload)
    }

  override def unlikePage: ServiceCall[UnlikePayload, Done] =
    ServiceCall { payload =>
      pageEntityService.unlikePage(payload)
    }

  override def getPageMetric: ServiceCall[GetMetricPayload, Metric] =
    ServiceCall { payload =>
      pageEntityService.getPageMetric(payload)
    }

  override def getPageMetrics: ServiceCall[GetMetricsPayload, Seq[Metric]] =
    ServiceCall { payload =>
      pageEntityService.getPageMetrics(payload)
    }

  // ************************** CMS Home Page  **************************

  override def assignHomePage: ServiceCall[AssignHomePagePayload, Done] =
    ServiceCall { payload =>
      homePageEntityService.assignHomePage(payload)
    }

  override def unassignHomePage: ServiceCall[UnassignHomePagePayload, Done] =
    ServiceCall { payload =>
      homePageEntityService.unassignHomePage(payload)
    }

  override def getHomePage(
    id: HomePageId,
    source: Option[String]
  ): ServiceCall[NotUsed, HomePage] =
    ServiceCall { _ =>
      homePageEntityService.getHomePage(id, source)
    }

  override def getHomePages(source: Option[String]): ServiceCall[Set[HomePageId], Seq[HomePage]] =
    ServiceCall { ids =>
      homePageEntityService.getHomePages(ids, source)
    }

  override def getHomePageByPrincipalCodes(applicationId: String): ServiceCall[Seq[String], PageId] =
    ServiceCall { principalCodes =>
      homePageEntityService.getHomePageByPrincipalCodes(applicationId, principalCodes)
    }

  override def findHomePages: ServiceCall[HomePageFindQuery, FindResult] =
    ServiceCall { query =>
      homePageEntityService.findHomePages(query)
    }

}
