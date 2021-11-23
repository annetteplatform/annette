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
  AssignTargetPrincipalPayload,
  CanAccessToEntityPayload,
  DeactivatePayload,
  DeletePayload,
  UnassignTargetPrincipalPayload,
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
  def getBlogCategoryById(id: CategoryId, fromReadSide: Boolean = true): Future[Category]
  def getBlogCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean = true): Future[Seq[Category]]
  def findBlogCategories(payload: CategoryFindQuery): Future[FindResult]

  def createBlog(payload: CreateBlogPayload): Future[Done]
  def updateBlogName(payload: UpdateNamePayload): Future[Done]
  def updateBlogDescription(payload: UpdateDescriptionPayload): Future[Done]
  def updateBlogCategoryId(payload: UpdateCategoryIdPayload): Future[Done]
  def assignBlogTargetPrincipal(payload: AssignTargetPrincipalPayload): Future[Done]
  def unassignBlogTargetPrincipal(payload: UnassignTargetPrincipalPayload): Future[Done]
  def activateBlog(payload: ActivatePayload): Future[Done]
  def deactivateBlog(payload: DeactivatePayload): Future[Done]
  def deleteBlog(payload: DeletePayload): Future[Done]
  def getBlogById(id: BlogId, fromReadSide: Boolean = true): Future[Blog]
  def getBlogsById(ids: Set[BlogId], fromReadSide: Boolean = true): Future[Seq[Blog]]
  def getBlogViews(payload: GetBlogViewsPayload): Future[Seq[BlogView]]
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
  def assignPostTargetPrincipal(payload: AssignTargetPrincipalPayload): Future[Updated]
  def unassignPostTargetPrincipal(payload: UnassignTargetPrincipalPayload): Future[Updated]
  def deletePost(payload: DeletePayload): Future[Updated]
  def getPostById(
    id: PostId,
    fromReadSide: Boolean = true,
    withIntro: Option[Boolean],
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Post]
  def getPostsById(
    ids: Set[PostId],
    fromReadSide: Boolean = true,
    withIntro: Option[Boolean],
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Seq[Post]]
  def getPostViews(payload: GetPostViewsPayload): Future[Seq[Post]]
  def canAccessToPost(payload: CanAccessToEntityPayload): Future[Boolean]
  def findPosts(query: PostFindQuery): Future[FindResult]

  def viewPost(payload: article.ViewPayload): Future[Done]
  def likePost(payload: LikePayload): Future[Done]
  def unlikePost(payload: UnlikePayload): Future[Done]
  def getPostMetricById(payload: GetMetricPayload): Future[Metric]
  def getPostMetricsById(payload: GetMetricsPayload): Future[Seq[Metric]]

  // ************************** CMS Pages **************************

  def createSpaceCategory(payload: CreateCategoryPayload): Future[Done]
  def updateSpaceCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteSpaceCategory(payload: DeleteCategoryPayload): Future[Done]
  def getSpaceCategoryById(id: CategoryId, fromReadSide: Boolean = true): Future[Category]
  def getSpaceCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean = true): Future[Seq[Category]]
  def findSpaceCategories(payload: CategoryFindQuery): Future[FindResult]

  def createSpace(payload: CreateSpacePayload): Future[Done]
  def updateSpaceName(payload: UpdateNamePayload): Future[Done]
  def updateSpaceDescription(payload: UpdateDescriptionPayload): Future[Done]
  def updateSpaceCategoryId(payload: UpdateCategoryIdPayload): Future[Done]
  def assignSpaceTargetPrincipal(payload: AssignTargetPrincipalPayload): Future[Done]
  def unassignSpaceTargetPrincipal(payload: UnassignTargetPrincipalPayload): Future[Done]
  def activateSpace(payload: ActivatePayload): Future[Done]
  def deactivateSpace(payload: DeactivatePayload): Future[Done]
  def deleteSpace(payload: DeletePayload): Future[Done]
  def getSpaceById(id: SpaceId, fromReadSide: Boolean = true): Future[Space]
  def getSpacesById(ids: Set[SpaceId], fromReadSide: Boolean = true): Future[Seq[Space]]
  def getSpaceViews(payload: GetSpaceViewsPayload): Future[Seq[SpaceView]]
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
  def assignPageTargetPrincipal(payload: AssignTargetPrincipalPayload): Future[Updated]
  def unassignPageTargetPrincipal(payload: UnassignTargetPrincipalPayload): Future[Updated]
  def deletePage(payload: DeletePayload): Future[Updated]
  def getPageById(
    id: PageId,
    fromReadSide: Boolean = true,
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Page]
  def getPagesById(
    ids: Set[PageId],
    fromReadSide: Boolean = true,
    withContent: Option[Boolean],
    withTargets: Option[Boolean]
  ): Future[Seq[Page]]
  def getPageViews(payload: GetPageViewsPayload): Future[Seq[Page]]
  def canAccessToPage(payload: CanAccessToEntityPayload): Future[Boolean]
  def findPages(query: PageFindQuery): Future[FindResult]

  def viewPage(payload: article.ViewPayload): Future[Done]
  def likePage(payload: LikePayload): Future[Done]
  def unlikePage(payload: UnlikePayload): Future[Done]
  def getPageMetricById(payload: GetMetricPayload): Future[Metric]
  def getPageMetricsById(payload: GetMetricsPayload): Future[Seq[Metric]]

}
