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

import akka.Done
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
  UpdateCategoryIdPayload,
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
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult

import scala.concurrent.Future

class CmsServiceImpl(api: CmsServiceApi) extends CmsService {

  // ************************** CMS Files **************************

  override def storeFile(payload: StoreFilePayload): Future[Done] =
    api.storeFile.invoke(payload)

  override def removeFile(payload: RemoveFilePayload): Future[Done] =
    api.removeFile.invoke(payload)

  override def removeFiles(payload: RemoveFilesPayload): Future[Done] =
    api.removeFiles.invoke(payload)

  override def getFiles(objectId: String): Future[Seq[FileDescriptor]] =
    api.getFiles(objectId).invoke()

  // ************************** CMS Blogs **************************

  override def createBlogCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createBlogCategory.invoke(payload)

  override def updateBlogCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateBlogCategory.invoke(payload)

  override def deleteBlogCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteBlogCategory.invoke(payload)

  override def getBlogCategory(id: CategoryId, fromReadSide: Boolean): Future[Category] =
    api.getBlogCategory(id, fromReadSide).invoke()

  override def getBlogCategories(ids: Set[CategoryId], fromReadSide: Boolean): Future[Seq[Category]] =
    api.getBlogCategories(fromReadSide).invoke(ids)

  override def findBlogCategories(query: CategoryFindQuery): Future[FindResult] =
    api.findBlogCategories.invoke(query)

  override def createBlog(payload: CreateBlogPayload): Future[Done] =
    api.createBlog.invoke(payload)

  override def updateBlogName(payload: UpdateNamePayload): Future[Done] =
    api.updateBlogName.invoke(payload)

  override def updateBlogDescription(payload: UpdateDescriptionPayload): Future[Done] =
    api.updateBlogDescription.invoke(payload)

  override def updateBlogCategoryId(payload: common.UpdateCategoryIdPayload): Future[Done] =
    api.updateBlogCategoryId.invoke(payload)

  override def assignBlogAuthorPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    api.assignBlogAuthorPrincipal.invoke(payload)

  override def unassignBlogAuthorPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    api.unassignBlogAuthorPrincipal.invoke(payload)

  override def assignBlogTargetPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    api.assignBlogTargetPrincipal.invoke(payload)

  override def unassignBlogTargetPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    api.unassignBlogTargetPrincipal.invoke(payload)

  override def activateBlog(payload: ActivatePayload): Future[Done] =
    api.activateBlog.invoke(payload)

  override def deactivateBlog(payload: DeactivatePayload): Future[Done] =
    api.deactivateBlog.invoke(payload)

  override def deleteBlog(payload: DeletePayload): Future[Done] =
    api.deleteBlog.invoke(payload)

  override def getBlog(id: BlogId, fromReadSide: Boolean): Future[Blog] =
    api.getBlog(id, fromReadSide).invoke()

  override def getBlogs(ids: Set[BlogId], fromReadSide: Boolean): Future[Seq[Blog]] =
    api.getBlogs(fromReadSide).invoke(ids)

  override def getBlogViews(
    payload: GetBlogViewsPayload
  ): Future[Seq[BlogView]] =
    api.getBlogViews.invoke(payload)

  override def canEditBlogPosts(payload: CanAccessToEntityPayload): Future[Boolean] =
    api.canEditBlogPosts.invoke(payload)

  override def canAccessToBlog(payload: CanAccessToEntityPayload): Future[Boolean] =
    api.canAccessToBlog.invoke(payload)

  override def findBlogs(query: BlogFindQuery): Future[FindResult] =
    api.findBlogs.invoke(query)

  override def createPost(payload: CreatePostPayload): Future[Post] =
    api.createPost.invoke(payload)

  override def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Updated] =
    api.updatePostFeatured.invoke(payload)

  override def updatePostAuthor(payload: UpdateAuthorPayload): Future[Updated] =
    api.updatePostAuthor.invoke(payload)

  override def updatePostTitle(payload: UpdateTitlePayload): Future[Updated] =
    api.updatePostTitle.invoke(payload)

  override def updatePostContentSettings(payload: UpdateContentSettingsPayload): Future[Updated] =
    api.updatePostContentSettings.invoke(payload)

  override def updatePostWidget(payload: UpdateWidgetPayload): Future[Updated] =
    api.updatePostWidget.invoke(payload)

  override def changePostWidgetOrder(payload: ChangeWidgetOrderPayload): Future[Updated] =
    api.changePostWidgetOrder.invoke(payload)

  override def deletePostWidget(payload: DeleteWidgetPayload): Future[Updated] =
    api.deletePostWidget.invoke(payload)

  override def updatePostPublicationTimestamp(payload: UpdatePublicationTimestampPayload): Future[Updated] =
    api.updatePostPublicationTimestamp.invoke(payload)

  override def publishPost(payload: PublishPayload): Future[Updated] =
    api.publishPost.invoke(payload)

  override def unpublishPost(payload: UnpublishPayload): Future[Updated] =
    api.unpublishPost.invoke(payload)

  override def assignPostTargetPrincipal(payload: AssignPrincipalPayload): Future[Updated] =
    api.assignPostTargetPrincipal.invoke(payload)

  override def unassignPostTargetPrincipal(payload: UnassignPrincipalPayload): Future[Updated] =
    api.unassignPostTargetPrincipal.invoke(payload)

  override def deletePost(payload: DeletePayload): Future[Updated] =
    api.deletePost.invoke(payload)

  override def getPost(
    id: PostId,
    fromReadSide: Boolean,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): Future[Post] =
    api.getPost(id, fromReadSide, withIntro, withContent, withTargets).invoke()

  override def getPosts(
    ids: Set[PostId],
    fromReadSide: Boolean,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): Future[Seq[Post]] =
    api.getPosts(fromReadSide, withIntro, withContent, withTargets).invoke(ids)

  override def getPostViews(payload: GetPostViewsPayload): Future[Seq[Post]] =
    api.getPostViews.invoke(payload)

  override def canEditPost(payload: CanAccessToEntityPayload): Future[Boolean] =
    api.canEditPost.invoke(payload)

  override def canAccessToPost(payload: CanAccessToEntityPayload): Future[Boolean] =
    api.canAccessToPost.invoke(payload)

  override def findPosts(query: PostFindQuery): Future[FindResult] =
    api.findPosts.invoke(query)

  override def viewPost(payload: article.ViewPayload): Future[Done] =
    api.viewPost.invoke(payload)

  override def likePost(payload: LikePayload): Future[Done] =
    api.likePost.invoke(payload)

  override def unlikePost(payload: UnlikePayload): Future[Done] =
    api.unlikePost.invoke(payload)

  override def getPostMetric(payload: GetMetricPayload): Future[Metric] =
    api.getPostMetric.invoke(payload)

  override def getPostMetrics(payload: GetMetricsPayload): Future[Seq[Metric]] =
    api.getPostMetrics.invoke(payload)

  // ************************** CMS Pages **************************

  override def createSpaceCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createSpaceCategory.invoke(payload)

  override def updateSpaceCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateSpaceCategory.invoke(payload)

  override def deleteSpaceCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteSpaceCategory.invoke(payload)

  override def getSpaceCategory(id: CategoryId, fromReadSide: Boolean): Future[Category] =
    api.getSpaceCategory(id, fromReadSide).invoke()

  override def getSpaceCategories(ids: Set[CategoryId], fromReadSide: Boolean): Future[Seq[Category]] =
    api.getSpaceCategories(fromReadSide).invoke(ids)

  override def findSpaceCategories(query: CategoryFindQuery): Future[FindResult] =
    api.findSpaceCategories.invoke(query)

  override def createSpace(payload: CreateSpacePayload): Future[Done] =
    api.createSpace.invoke(payload)

  override def updateSpaceName(payload: UpdateNamePayload): Future[Done] =
    api.updateSpaceName.invoke(payload)

  override def updateSpaceDescription(payload: UpdateDescriptionPayload): Future[Done] =
    api.updateSpaceDescription.invoke(payload)

  override def updateSpaceCategoryId(payload: UpdateCategoryIdPayload): Future[Done] =
    api.updateSpaceCategoryId.invoke(payload)

  override def assignSpaceAuthorPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    api.assignSpaceAuthorPrincipal.invoke(payload)

  override def unassignSpaceAuthorPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    api.unassignSpaceAuthorPrincipal.invoke(payload)

  override def assignSpaceTargetPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    api.assignSpaceTargetPrincipal.invoke(payload)

  override def unassignSpaceTargetPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    api.unassignSpaceTargetPrincipal.invoke(payload)

  override def activateSpace(payload: ActivatePayload): Future[Done] =
    api.activateSpace.invoke(payload)

  override def deactivateSpace(payload: DeactivatePayload): Future[Done] =
    api.deactivateSpace.invoke(payload)

  override def deleteSpace(payload: DeletePayload): Future[Done] =
    api.deleteSpace.invoke(payload)

  override def getSpace(id: SpaceId, fromReadSide: Boolean): Future[Space] =
    api.getSpace(id, fromReadSide).invoke()

  override def getSpaces(ids: Set[SpaceId], fromReadSide: Boolean): Future[Seq[Space]] =
    api.getSpaces(fromReadSide).invoke(ids)

  override def getSpaceViews(
    payload: GetSpaceViewsPayload
  ): Future[Seq[SpaceView]] =
    api.getSpaceViews.invoke(payload)

  override def canEditSpacePages(payload: CanAccessToEntityPayload): Future[Boolean] =
    api.canEditSpacePages.invoke(payload)

  override def canAccessToSpace(payload: CanAccessToEntityPayload): Future[Boolean] =
    api.canAccessToSpace.invoke(payload)

  override def findSpaces(query: SpaceFindQuery): Future[FindResult] =
    api.findSpaces.invoke(query)

  override def createPage(payload: CreatePagePayload): Future[Page] =
    api.createPage.invoke(payload)

  override def updatePageAuthor(payload: UpdateAuthorPayload): Future[Updated] =
    api.updatePageAuthor.invoke(payload)

  override def updatePageTitle(payload: UpdateTitlePayload): Future[Updated] =
    api.updatePageTitle.invoke(payload)

  override def updatePageContentSettings(payload: UpdateContentSettingsPayload): Future[Updated] =
    api.updatePageContentSettings.invoke(payload)

  override def updatePageWidget(payload: UpdateWidgetPayload): Future[Updated] =
    api.updatePageWidget.invoke(payload)

  override def changePageWidgetOrder(payload: ChangeWidgetOrderPayload): Future[Updated] =
    api.changePageWidgetOrder.invoke(payload)

  override def deletePageWidget(payload: DeleteWidgetPayload): Future[Updated] =
    api.deletePageWidget.invoke(payload)

  override def updatePagePublicationTimestamp(payload: UpdatePublicationTimestampPayload): Future[Updated] =
    api.updatePagePublicationTimestamp.invoke(payload)

  override def publishPage(payload: PublishPayload): Future[Updated] =
    api.publishPage.invoke(payload)

  override def unpublishPage(payload: UnpublishPayload): Future[Updated] =
    api.unpublishPage.invoke(payload)

  override def assignPageTargetPrincipal(payload: AssignPrincipalPayload): Future[Updated] =
    api.assignPageTargetPrincipal.invoke(payload)

  override def unassignPageTargetPrincipal(payload: UnassignPrincipalPayload): Future[Updated] =
    api.unassignPageTargetPrincipal.invoke(payload)

  override def deletePage(payload: DeletePayload): Future[Updated] =
    api.deletePage.invoke(payload)

  override def getPage(
    id: PageId,
    fromReadSide: Boolean,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): Future[Page] =
    api.getPage(id, fromReadSide, withContent, withTargets).invoke()

  override def getPages(
    ids: Set[PageId],
    fromReadSide: Boolean,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): Future[Seq[Page]] =
    api.getPages(fromReadSide, withContent, withTargets).invoke(ids)

  override def getPageViews(payload: GetPageViewsPayload): Future[Seq[Page]] =
    api.getPageViews.invoke(payload)

  override def canEditPage(payload: CanAccessToEntityPayload): Future[Boolean] =
    api.canEditPage.invoke(payload)

  override def canAccessToPage(payload: CanAccessToEntityPayload): Future[Boolean] =
    api.canAccessToPage.invoke(payload)

  override def findPages(query: PageFindQuery): Future[FindResult] =
    api.findPages.invoke(query)

  override def viewPage(payload: article.ViewPayload): Future[Done] =
    api.viewPage.invoke(payload)

  override def likePage(payload: LikePayload): Future[Done] =
    api.likePage.invoke(payload)

  override def unlikePage(payload: UnlikePayload): Future[Done] =
    api.unlikePage.invoke(payload)

  override def getPageMetric(payload: GetMetricPayload): Future[Metric] =
    api.getPageMetric.invoke(payload)

  override def getPageMetrics(payload: GetMetricsPayload): Future[Seq[Metric]] =
    api.getPageMetrics.invoke(payload)

  // ************************** CMS Home Page  **************************

  override def assignHomePage(payload: AssignHomePagePayload): Future[Done] =
    api.assignHomePage.invoke(payload)

  override def unassignHomePage(payload: UnassignHomePagePayload): Future[Done] =
    api.unassignHomePage.invoke(payload)

  override def getHomePage(id: HomePageId, fromReadSide: Boolean = true): Future[HomePage] =
    api.getHomePage(id, fromReadSide).invoke()

  override def getHomePages(
    ids: Set[HomePageId],
    fromReadSide: Boolean = true
  ): Future[Seq[HomePage]] =
    api.getHomePages(fromReadSide).invoke(ids)

  override def getHomePageByPrincipalCodes(applicationId: String, ids: Seq[String]): Future[PageId] =
    api.getHomePageByPrincipalCodes(applicationId).invoke(ids)

  override def findHomePages(query: HomePageFindQuery): Future[FindResult] =
    api.findHomePages.invoke(query)

}
