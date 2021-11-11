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
import biz.lobachev.annette.cms.api.files.{
  FileDescriptor,
  RemoveFilePayload,
  RemoveFilesPayload,
  StoreFilePayload,
  UpdateFileNamePayload
}
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult

import scala.concurrent.Future

class CmsServiceImpl(api: CmsServiceApi) extends CmsService {
  override def createBlogCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createBlogCategory.invoke(payload)

  override def updateBlogCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateBlogCategory.invoke(payload)

  override def deleteBlogCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteBlogCategory.invoke(payload)

  override def getBlogCategoryById(id: CategoryId, fromReadSide: Boolean): Future[Category] =
    api.getBlogCategoryById(id, fromReadSide).invoke()

  override def getBlogCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean): Future[Seq[Category]] =
    api.getBlogCategoriesById(fromReadSide).invoke(ids)

  override def findBlogCategories(query: CategoryFindQuery): Future[FindResult] =
    api.findBlogCategories.invoke(query)

  override def createBlog(payload: CreateBlogPayload): Future[Done] =
    api.createBlog.invoke(payload)

  override def updateBlogName(payload: UpdateBlogNamePayload): Future[Done] =
    api.updateBlogName.invoke(payload)

  override def updateBlogDescription(payload: UpdateBlogDescriptionPayload): Future[Done] =
    api.updateBlogDescription.invoke(payload)

  override def updateBlogCategoryId(payload: UpdateBlogCategoryPayload): Future[Done] =
    api.updateBlogCategoryId.invoke(payload)

  override def assignBlogTargetPrincipal(payload: AssignBlogTargetPrincipalPayload): Future[Done] =
    api.assignBlogTargetPrincipal.invoke(payload)

  override def unassignBlogTargetPrincipal(payload: UnassignBlogTargetPrincipalPayload): Future[Done] =
    api.unassignBlogTargetPrincipal.invoke(payload)

  override def activateBlog(payload: ActivateBlogPayload): Future[Done] =
    api.activateBlog.invoke(payload)

  override def deactivateBlog(payload: DeactivateBlogPayload): Future[Done] =
    api.deactivateBlog.invoke(payload)

  override def deleteBlog(payload: DeleteBlogPayload): Future[Done] =
    api.deleteBlog.invoke(payload)

  override def getBlogById(id: BlogId, fromReadSide: Boolean): Future[Blog] =
    api.getBlogById(id, fromReadSide).invoke()

  override def getBlogsById(ids: Set[BlogId], fromReadSide: Boolean): Future[Seq[Blog]] =
    api.getBlogsById(fromReadSide).invoke(ids)

  override def getBlogViews(
    payload: GetBlogViewsPayload
  ): Future[Seq[BlogView]] =
    api.getBlogViews.invoke(payload)

  override def canAccessToBlog(payload: CanAccessToBlogPayload): Future[Boolean] =
    api.canAccessToBlog.invoke(payload)

  override def findBlogs(query: BlogFindQuery): Future[FindResult] =
    api.findBlogs.invoke(query)

  override def createPost(payload: CreatePostPayload): Future[Done] =
    api.createPost.invoke(payload)

  override def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Done] =
    api.updatePostFeatured.invoke(payload)

  override def updatePostAuthor(payload: UpdatePostAuthorPayload): Future[Done] =
    api.updatePostAuthor.invoke(payload)

  override def updatePostTitle(payload: UpdatePostTitlePayload): Future[Done] =
    api.updatePostTitle.invoke(payload)

  override def updatePostWidgetContent(payload: UpdatePostWidgetContentPayload): Future[Done] =
    api.updatePostWidgetContent.invoke(payload)

  override def changePostWidgetContentOrder(payload: ChangePostWidgetContentOrderPayload): Future[Done] =
    api.changePostWidgetContentOrder.invoke(payload)

  override def deletePostWidgetContent(payload: DeletePostWidgetContentPayload): Future[Done] =
    api.deletePostWidgetContent.invoke(payload)

  override def updatePostPublicationTimestamp(payload: UpdatePostPublicationTimestampPayload): Future[Done] =
    api.updatePostPublicationTimestamp.invoke(payload)

  override def publishPost(payload: PublishPostPayload): Future[Done] =
    api.publishPost.invoke(payload)

  override def unpublishPost(payload: UnpublishPostPayload): Future[Done] =
    api.unpublishPost.invoke(payload)

  override def assignPostTargetPrincipal(payload: AssignPostTargetPrincipalPayload): Future[Done] =
    api.assignPostTargetPrincipal.invoke(payload)

  override def unassignPostTargetPrincipal(payload: UnassignPostTargetPrincipalPayload): Future[Done] =
    api.unassignPostTargetPrincipal.invoke(payload)

  override def deletePost(payload: DeletePostPayload): Future[Done] =
    api.deletePost.invoke(payload)

  override def getPostById(id: PostId, fromReadSide: Boolean): Future[Post] =
    api.getPostById(id, fromReadSide).invoke()

  override def getPostAnnotationById(id: PostId, fromReadSide: Boolean): Future[PostAnnotation] =
    api.getPostAnnotationById(id, fromReadSide).invoke()

  override def getPostsById(ids: Set[PostId], fromReadSide: Boolean): Future[Seq[Post]] =
    api.getPostsById(fromReadSide).invoke(ids)

  override def getPostAnnotationsById(ids: Set[PostId], fromReadSide: Boolean): Future[Seq[PostAnnotation]] =
    api.getPostAnnotationsById(fromReadSide).invoke(ids)

  override def getPostViews(payload: GetPostViewsPayload): Future[Seq[PostView]] =
    api.getPostViews.invoke(payload)

  override def canAccessToPost(payload: CanAccessToPostPayload): Future[Boolean] =
    api.canAccessToPost.invoke(payload)

  override def findPosts(query: PostFindQuery): Future[FindResult] =
    api.findPosts.invoke(query)

  override def viewPost(payload: ViewPostPayload): Future[Done] =
    api.viewPost.invoke(payload)

  override def likePost(payload: LikePostPayload): Future[Done] =
    api.likePost.invoke(payload)

  override def unlikePost(payload: UnlikePostPayload): Future[Done] =
    api.unlikePost.invoke(payload)

  override def getPostMetricById(payload: GetPostMetricPayload): Future[PostMetric] =
    api.getPostMetricById.invoke(payload)

  override def getPostMetricsById(payload: GetPostMetricsPayload): Future[Seq[PostMetric]] =
    api.getPostMetricsById.invoke(payload)

  override def storeFile(payload: StoreFilePayload): Future[Done] =
    api.storeFile.invoke(payload)

  override def updateFileName(payload: UpdateFileNamePayload): Future[Done] =
    api.updateFileName.invoke(payload)

  override def removeFile(payload: RemoveFilePayload): Future[Done] =
    api.removeFile.invoke(payload)

  override def removeFiles(payload: RemoveFilesPayload): Future[Done] =
    api.removeFiles.invoke(payload)

  override def getFiles(objectId: String): Future[Seq[FileDescriptor]] =
    api.getFiles(objectId).invoke()
}
