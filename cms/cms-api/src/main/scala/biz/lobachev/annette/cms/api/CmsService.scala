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
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult

import scala.concurrent.Future

trait CmsService {

  def createBlogCategory(payload: CreateCategoryPayload): Future[Done]
  def updateBlogCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteBlogCategory(payload: DeleteCategoryPayload): Future[Done]
  def getBlogCategoryById(id: CategoryId, fromReadSide: Boolean = true): Future[Category]
  def getBlogCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean = true): Future[Seq[Category]]
  def findBlogCategories(payload: CategoryFindQuery): Future[FindResult]

  def createBlog(payload: CreateBlogPayload): Future[Done]
  def updateBlogName(payload: UpdateBlogNamePayload): Future[Done]
  def updateBlogDescription(payload: UpdateBlogDescriptionPayload): Future[Done]
  def updateBlogCategoryId(payload: UpdateBlogCategoryPayload): Future[Done]
  def assignBlogTargetPrincipal(payload: AssignBlogTargetPrincipalPayload): Future[Done]
  def unassignBlogTargetPrincipal(payload: UnassignBlogTargetPrincipalPayload): Future[Done]
  def activateBlog(payload: ActivateBlogPayload): Future[Done]
  def deactivateBlog(payload: DeactivateBlogPayload): Future[Done]
  def deleteBlog(payload: DeleteBlogPayload): Future[Done]
  def getBlogById(id: BlogId, fromReadSide: Boolean = true): Future[Blog]
  def getBlogsById(ids: Set[BlogId], fromReadSide: Boolean = true): Future[Seq[Blog]]
  def getBlogViews(payload: GetBlogViewsPayload): Future[Seq[BlogView]]
  def canAccessToBlog(payload: CanAccessToBlogPayload): Future[Boolean]
  def findBlogs(payload: BlogFindQuery): Future[FindResult]

  def createPost(payload: CreatePostPayload): Future[Done]
  def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Done]
  def updatePostAuthor(payload: UpdatePostAuthorPayload): Future[Done]
  def updatePostTitle(payload: UpdatePostTitlePayload): Future[Done]
  def updatePostWidgetContent(payload: UpdatePostWidgetContentPayload): Future[Done]
  def changePostWidgetContentOrder(payload: ChangePostWidgetContentOrderPayload): Future[Done]
  def deletePostWidgetContent(payload: DeletePostWidgetContentPayload): Future[Done]
  def updatePostPublicationTimestamp(payload: UpdatePostPublicationTimestampPayload): Future[Done]
  def publishPost(payload: PublishPostPayload): Future[Done]
  def unpublishPost(payload: UnpublishPostPayload): Future[Done]
  def assignPostTargetPrincipal(payload: AssignPostTargetPrincipalPayload): Future[Done]
  def unassignPostTargetPrincipal(payload: UnassignPostTargetPrincipalPayload): Future[Done]
  def deletePost(payload: DeletePostPayload): Future[Done]
  def getPostById(id: PostId, fromReadSide: Boolean = true): Future[Post]
  def getPostAnnotationById(id: PostId, fromReadSide: Boolean = true): Future[PostAnnotation]
  def getPostsById(ids: Set[PostId], fromReadSide: Boolean = true): Future[Seq[Post]]
  def getPostAnnotationsById(ids: Set[PostId], fromReadSide: Boolean = true): Future[Seq[PostAnnotation]]
  def getPostViews(payload: GetPostViewsPayload): Future[Seq[PostView]]
  def canAccessToPost(payload: CanAccessToPostPayload): Future[Boolean]
  def findPosts(query: PostFindQuery): Future[FindResult]
  def storePostMedia(payload: StorePostMediaPayload): Future[Done]
  def removePostMedia(payload: RemovePostMediaPayload): Future[Done]
  def storePostDoc(payload: StorePostDocPayload): Future[Done]
  def removePostDoc(payload: RemovePostDocPayload): Future[Done]

  def viewPost(payload: ViewPostPayload): Future[Done]
  def likePost(payload: LikePostPayload): Future[Done]
  def unlikePost(payload: UnlikePostPayload): Future[Done]
  def getPostMetricById(payload: GetPostMetricPayload): Future[PostMetric]
  def getPostMetricsById(payload: GetPostMetricsPayload): Future[Seq[PostMetric]]

}
