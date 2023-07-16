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
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.files.{FileDescriptor, RemoveFilePayload, RemoveFilesPayload, StoreFilePayload}
import biz.lobachev.annette.cms.api.home_pages.{
  AssignHomePagePayload,
  HomePage,
  HomePageFindQuery,
  HomePageId,
  UnassignHomePagePayload
}
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult

import scala.concurrent.Future

trait CmsService {

  // ************************** CMS Files **************************

  def storeFile(payload: StoreFilePayload): Future[Done]
  def removeFile(payload: RemoveFilePayload): Future[Done]
  def removeFiles(payload: RemoveFilesPayload): Future[Done]
  def getFiles(objectId: String): Future[Seq[FileDescriptor]]

  // ************************** CMS Blogs **************************

  def createBlogCategory(payload: CreateCategoryPayload): Future[Done]
  def updateBlogCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteBlogCategory(payload: DeleteCategoryPayload): Future[Done]
  def getBlogCategory(id: CategoryId, source: Option[String] = None): Future[Category]
  def getBlogCategories(ids: Set[CategoryId], source: Option[String] = None): Future[Seq[Category]]
  def findBlogCategories(payload: CategoryFindQuery): Future[FindResult]

  def createBlog(payload: CreateBlogPayload): Future[Done]
  def updateBlogName(payload: UpdateNamePayload): Future[Done]
  def updateBlogDescription(payload: UpdateDescriptionPayload): Future[Done]
  def updateBlogCategoryId(payload: UpdateCategoryIdPayload): Future[Done]
  def assignBlogAuthorPrincipal(payload: AssignPrincipalPayload): Future[Done]
  def unassignBlogAuthorPrincipal(payload: UnassignPrincipalPayload): Future[Done]
  def assignBlogTargetPrincipal(payload: AssignPrincipalPayload): Future[Done]
  def unassignBlogTargetPrincipal(payload: UnassignPrincipalPayload): Future[Done]
  def activateBlog(payload: ActivatePayload): Future[Done]
  def deactivateBlog(payload: DeactivatePayload): Future[Done]
  def deleteBlog(payload: DeletePayload): Future[Done]
  def getBlog(id: BlogId, source: Option[String] = None): Future[Blog]
  def getBlogs(ids: Set[BlogId], source: Option[String] = None): Future[Seq[Blog]]
  def getBlogViews(payload: GetBlogViewsPayload): Future[Seq[BlogView]]
  def canEditBlogPosts(payload: CanAccessToEntityPayload): Future[Boolean]
  def canAccessToBlog(payload: CanAccessToEntityPayload): Future[Boolean]
  def findBlogs(payload: BlogFindQuery): Future[FindResult]

  def createPost(payload: CreatePostPayload): Future[Post]
  def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Updated]
  def updatePostAuthor(payload: UpdateAuthorPayload): Future[Updated]
  def updatePostTitle(payload: UpdateTitlePayload): Future[Updated]
  def updatePostContentSettings(payload: UpdateContentSettingsPayload): Future[Updated]
  def updatePostWidget(payload: UpdateWidgetPayload): Future[Updated]
  def changePostWidgetOrder(payload: ChangeWidgetOrderPayload): Future[Updated]
  def deletePostWidget(payload: DeleteWidgetPayload): Future[Updated]
  def updatePostPublicationTimestamp(payload: UpdatePublicationTimestampPayload): Future[Updated]
  def publishPost(payload: PublishPayload): Future[Updated]
  def unpublishPost(payload: UnpublishPayload): Future[Updated]
  def assignPostTargetPrincipal(payload: AssignPrincipalPayload): Future[Updated]
  def unassignPostTargetPrincipal(payload: UnassignPrincipalPayload): Future[Updated]
  def deletePost(payload: DeletePayload): Future[Updated]
  def getPost(
    id: PostId,
    source: Option[String] = None,
    withIntro: Option[Boolean],
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Post]
  def getPosts(
    ids: Set[PostId],
    source: Option[String] = None,
    withIntro: Option[Boolean],
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Seq[Post]]
  def getPostViews(payload: GetPostViewsPayload): Future[Seq[Post]]
  def canEditPost(payload: CanAccessToEntityPayload): Future[Boolean]
  def canAccessToPost(payload: CanAccessToEntityPayload): Future[Boolean]
  def findPosts(query: PostFindQuery): Future[FindResult]

  def viewPost(payload: article.ViewPayload): Future[Done]
  def likePost(payload: LikePayload): Future[Done]
  def unlikePost(payload: UnlikePayload): Future[Done]
  def getPostMetric(payload: GetMetricPayload): Future[Metric]
  def getPostMetrics(payload: GetMetricsPayload): Future[Seq[Metric]]

  // ************************** CMS Pages **************************

  def createSpaceCategory(payload: CreateCategoryPayload): Future[Done]
  def updateSpaceCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteSpaceCategory(payload: DeleteCategoryPayload): Future[Done]
  def getSpaceCategory(id: CategoryId, source: Option[String] = None): Future[Category]
  def getSpaceCategories(ids: Set[CategoryId], source: Option[String] = None): Future[Seq[Category]]
  def findSpaceCategories(payload: CategoryFindQuery): Future[FindResult]

  def createSpace(payload: CreateSpacePayload): Future[Done]
  def updateSpaceName(payload: UpdateNamePayload): Future[Done]
  def updateSpaceDescription(payload: UpdateDescriptionPayload): Future[Done]
  def updateSpaceCategoryId(payload: UpdateCategoryIdPayload): Future[Done]
  def assignSpaceAuthorPrincipal(payload: AssignPrincipalPayload): Future[Done]
  def unassignSpaceAuthorPrincipal(payload: UnassignPrincipalPayload): Future[Done]
  def assignSpaceTargetPrincipal(payload: AssignPrincipalPayload): Future[Done]
  def unassignSpaceTargetPrincipal(payload: UnassignPrincipalPayload): Future[Done]
  def activateSpace(payload: ActivatePayload): Future[Done]
  def deactivateSpace(payload: DeactivatePayload): Future[Done]
  def deleteSpace(payload: DeletePayload): Future[Done]
  def getSpace(id: SpaceId, source: Option[String] = None): Future[Space]
  def getSpaces(ids: Set[SpaceId], source: Option[String] = None): Future[Seq[Space]]
  def getSpaceViews(payload: GetSpaceViewsPayload): Future[Seq[SpaceView]]
  def canEditSpacePages(payload: CanAccessToEntityPayload): Future[Boolean]
  def canAccessToSpace(payload: CanAccessToEntityPayload): Future[Boolean]
  def findSpaces(payload: SpaceFindQuery): Future[FindResult]

  def createPage(payload: CreatePagePayload): Future[Page]
  def updatePageAuthor(payload: UpdateAuthorPayload): Future[Updated]
  def updatePageTitle(payload: UpdateTitlePayload): Future[Updated]
  def updatePageContentSettings(payload: UpdateContentSettingsPayload): Future[Updated]
  def updatePageWidget(payload: UpdateWidgetPayload): Future[Updated]
  def changePageWidgetOrder(payload: ChangeWidgetOrderPayload): Future[Updated]
  def deletePageWidget(payload: DeleteWidgetPayload): Future[Updated]
  def updatePagePublicationTimestamp(payload: UpdatePublicationTimestampPayload): Future[Updated]
  def publishPage(payload: PublishPayload): Future[Updated]
  def unpublishPage(payload: UnpublishPayload): Future[Updated]
  def assignPageTargetPrincipal(payload: AssignPrincipalPayload): Future[Updated]
  def unassignPageTargetPrincipal(payload: UnassignPrincipalPayload): Future[Updated]
  def deletePage(payload: DeletePayload): Future[Updated]
  def getPage(
    id: PageId,
    source: Option[String] = None,
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Page]
  def getPages(
    ids: Set[PageId],
    source: Option[String] = None,
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Seq[Page]]
  def getPageViews(payload: GetPageViewsPayload): Future[Seq[Page]]
  def canEditPage(payload: CanAccessToEntityPayload): Future[Boolean]
  def canAccessToPage(payload: CanAccessToEntityPayload): Future[Boolean]
  def findPages(query: PageFindQuery): Future[FindResult]

  def viewPage(payload: article.ViewPayload): Future[Done]
  def likePage(payload: LikePayload): Future[Done]
  def unlikePage(payload: UnlikePayload): Future[Done]
  def getPageMetric(payload: GetMetricPayload): Future[Metric]
  def getPageMetrics(payload: GetMetricsPayload): Future[Seq[Metric]]

  // ************************** CMS Home Page  **************************

  def assignHomePage(payload: AssignHomePagePayload): Future[Done]
  def unassignHomePage(payload: UnassignHomePagePayload): Future[Done]
  def getHomePage(
    id: HomePageId,
    source: Option[String] = None
  ): Future[HomePage]
  def getHomePages(ids: Set[HomePageId], source: Option[String] = None): Future[Seq[HomePage]]
  def getHomePageByPrincipalCodes(applicationId: String, ids: Seq[String]): Future[PageId]
  def findHomePages(query: HomePageFindQuery): Future[FindResult]

}
