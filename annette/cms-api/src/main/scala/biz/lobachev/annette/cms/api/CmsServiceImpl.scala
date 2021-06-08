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
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.api.category._
import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.core.model.elastic.FindResult

import scala.collection.immutable.Map
import scala.concurrent.Future

class CmsServiceImpl(api: CmsServiceApi) extends CmsService {
  override def createCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createCategory.invoke(payload)

  override def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateCategory.invoke(payload)

  override def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteCategory.invoke(payload)

  override def getCategoryById(id: CategoryId, fromReadSide: Boolean): Future[Category] =
    api.getCategoryById(id, fromReadSide).invoke()

  override def getCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean): Future[Map[CategoryId, Category]] =
    api.getCategoriesById(fromReadSide).invoke(ids)

  override def findCategories(query: CategoryFindQuery): Future[FindResult] =
    api.findCategories.invoke(query)

  override def createSpace(payload: CreateSpacePayload): Future[Done] =
    api.createSpace.invoke(payload)

  override def updateSpaceName(payload: UpdateSpaceNamePayload): Future[Done] =
    api.updateSpaceName.invoke(payload)

  override def updateSpaceDescription(payload: UpdateSpaceDescriptionPayload): Future[Done] =
    api.updateSpaceDescription.invoke(payload)

  override def updateSpaceCategory(payload: UpdateSpaceCategoryPayload): Future[Done] =
    api.updateSpaceCategory.invoke(payload)

  override def assignSpaceTargetPrincipal(payload: AssignSpaceTargetPrincipalPayload): Future[Done] =
    api.assignSpaceTargetPrincipal.invoke(payload)

  override def unassignSpaceTargetPrincipal(payload: UnassignSpaceTargetPrincipalPayload): Future[Done] =
    api.unassignSpaceTargetPrincipal.invoke(payload)

  override def activateSpace(payload: ActivateSpacePayload): Future[Done] =
    api.activateSpace.invoke(payload)

  override def deactivateSpace(payload: DeactivateSpacePayload): Future[Done] =
    api.deactivateSpace.invoke(payload)

  override def deleteSpace(payload: DeleteSpacePayload): Future[Done] =
    api.deleteSpace.invoke(payload)

  override def getSpaceById(id: SpaceId, fromReadSide: Boolean): Future[Space] =
    api.getSpaceById(id, fromReadSide).invoke()

  override def getSpacesById(ids: Set[SpaceId], fromReadSide: Boolean): Future[Map[SpaceId, Space]] =
    api.getSpacesById(fromReadSide).invoke(ids)

  override def getSpaceViews(
    payload: GetSpaceViewsPayload
  ): Future[Map[SpaceId, SpaceView]] =
    api.getSpaceViews.invoke(payload)

  override def findSpaces(query: SpaceFindQuery): Future[FindResult] =
    api.findSpaces.invoke(query)

  override def createPost(payload: CreatePostPayload): Future[Done] =
    api.createPost.invoke(payload)

  override def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Done] =
    api.updatePostFeatured.invoke(payload)

  override def updatePostAuthor(payload: UpdatePostAuthorPayload): Future[Done] =
    api.updatePostAuthor.invoke(payload)

  override def updatePostTitle(payload: UpdatePostTitlePayload): Future[Done] =
    api.updatePostTitle.invoke(payload)

  override def updatePostIntro(payload: UpdatePostIntroPayload): Future[Done] =
    api.updatePostIntro.invoke(payload)

  override def updatePostContent(payload: UpdatePostContentPayload): Future[Done] =
    api.updatePostContent.invoke(payload)

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

  override def getPostsById(ids: Set[PostId], fromReadSide: Boolean): Future[Map[PostId, Post]] =
    api.getPostsById(fromReadSide).invoke(ids)

  override def getPostAnnotationsById(ids: Set[PostId], fromReadSide: Boolean): Future[Map[PostId, PostAnnotation]] =
    api.getPostAnnotationsById(fromReadSide).invoke(ids)

  override def getPostViews(payload: GetPostViewsPayload): Future[Map[PostId, PostView]] =
    api.getPostViews.invoke(payload)

  override def canAccessToPost(payload: CanAccessToPostPayload): Future[Boolean] =
    api.canAccessToPost.invoke(payload)

  override def findPosts(query: PostFindQuery): Future[FindResult] =
    api.findPosts.invoke(query)

  override def addPostMedia(payload: AddPostMediaPayload): Future[Done] =
    api.addPostMedia.invoke(payload)

  override def removePostMedia(payload: RemovePostMediaPayload): Future[Done] =
    api.removePostMedia.invoke(payload)

  override def addPostDoc(payload: AddPostDocPayload): Future[Done] =
    api.addPostDoc.invoke(payload)

  override def updatePostDocName(payload: UpdatePostDocNamePayload): Future[Done] =
    api.updatePostDocName.invoke(payload)

  override def removePostDoc(payload: RemovePostDocPayload): Future[Done] =
    api.removePostDoc.invoke(payload)

  override def viewPost(payload: ViewPostPayload): Future[Done] =
    api.viewPost.invoke(payload)

  override def likePost(payload: LikePostPayload): Future[Done] =
    api.likePost.invoke(payload)

  override def unlikePost(payload: UnlikePostPayload): Future[Done] =
    api.unlikePost.invoke(payload)

  override def getPostMetricById(payload: GetPostMetricPayload): Future[PostMetric] =
    api.getPostMetricById.invoke(payload)

  override def getPostMetricsById(payload: GetPostMetricsPayload): Future[Map[PostId, PostMetric]] =
    api.getPostMetricsById.invoke(payload)

  override def movePost(payload: MovePostPayload): Future[Done] =
    api.movePost.invoke(payload)

  override def getWikiHierarchyById(id: SpaceId, fromReadSide: Boolean): Future[WikiHierarchy] =
    api.getWikiHierarchyById(id, fromReadSide).invoke()

}
