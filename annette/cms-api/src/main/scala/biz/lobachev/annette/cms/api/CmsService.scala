package biz.lobachev.annette.cms.api

import akka.Done
import biz.lobachev.annette.cms.api.space.{
  ActivateSpacePayload,
  AssignSpaceTargetPrincipalPayload,
  CreateSpacePayload,
  DeactivateSpacePayload,
  DeleteSpacePayload,
  Space,
  SpaceAnnotation,
  SpaceFindQuery,
  SpaceId,
  UnassignSpaceTargetPrincipalPayload,
  UpdateSpaceCategoryPayload,
  UpdateSpaceDescriptionPayload,
  UpdateSpaceNamePayload
}
import biz.lobachev.annette.cms.api.category.{
  Category,
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.cms.api.post.{
  AddPostDocPayload,
  AddPostMediaPayload,
  AssignPostTargetPrincipalPayload,
  CreatePostPayload,
  DeletePostPayload,
  LikePostPayload,
  Post,
  PostAnnotation,
  PostFindQuery,
  PostId,
  PostMetric,
  PublishPostPayload,
  RemovePostDocPayload,
  RemovePostMediaPayload,
  UnassignPostTargetPrincipalPayload,
  UnpublishPostPayload,
  UpdatePostAuthorPayload,
  UpdatePostContentPayload,
  UpdatePostDocNamePayload,
  UpdatePostFeaturedPayload,
  UpdatePostIntroPayload,
  UpdatePostPublicationTimestampPayload,
  UpdatePostTitlePayload,
  ViewPostPayload
}
import biz.lobachev.annette.core.model.elastic.FindResult

import scala.collection.immutable.Map
import scala.concurrent.Future

trait CmsService {

  def createCategory(payload: CreateCategoryPayload): Future[Done]
  def updateCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteCategory(payload: DeleteCategoryPayload): Future[Done]
  def getCategoryById(id: CategoryId, fromReadSide: Boolean = true): Future[Category]
  def getCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean = true): Future[Map[CategoryId, Category]]
  def findCategories(payload: CategoryFindQuery): Future[FindResult]

  def createSpace(payload: CreateSpacePayload): Future[Done]
  def updateSpaceName(payload: UpdateSpaceNamePayload): Future[Done]
  def updateSpaceDescription(payload: UpdateSpaceDescriptionPayload): Future[Done]
  def updateSpaceCategory(payload: UpdateSpaceCategoryPayload): Future[Done]
  def assignSpaceTargetPrincipal(payload: AssignSpaceTargetPrincipalPayload): Future[Done]
  def unassignSpaceTargetPrincipal(payload: UnassignSpaceTargetPrincipalPayload): Future[Done]
  def activateSpace(payload: ActivateSpacePayload): Future[Done]
  def deactivateSpace(payload: DeactivateSpacePayload): Future[Done]
  def deleteSpace(payload: DeleteSpacePayload): Future[Done]
  def getSpaceById(id: SpaceId, fromReadSide: Boolean = true): Future[Space]
  def getSpaceAnnotationById(id: SpaceId, fromReadSide: Boolean = true): Future[SpaceAnnotation]
  def getSpacesById(ids: Set[SpaceId], fromReadSide: Boolean = true): Future[Map[SpaceId, Space]]
  def getSpaceAnnotationsById(ids: Set[SpaceId], fromReadSide: Boolean = true): Future[Map[SpaceId, SpaceAnnotation]]
  def findSpaces(payload: SpaceFindQuery): Future[FindResult]

  def createPost(payload: CreatePostPayload): Future[Done]
  def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Done]
  def updatePostAuthor(payload: UpdatePostAuthorPayload): Future[Done]
  def updatePostTitle(payload: UpdatePostTitlePayload): Future[Done]
  def updatePostIntro(payload: UpdatePostIntroPayload): Future[Done]
  def updatePostContent(payload: UpdatePostContentPayload): Future[Done]
  def updatePostPublicationTimestamp(payload: UpdatePostPublicationTimestampPayload): Future[Done]
  def publishPost(payload: PublishPostPayload): Future[Done]
  def unpublishPost(payload: UnpublishPostPayload): Future[Done]
  def assignPostTargetPrincipal(payload: AssignPostTargetPrincipalPayload): Future[Done]
  def unassignPostTargetPrincipal(payload: UnassignPostTargetPrincipalPayload): Future[Done]
  def deletePost(payload: DeletePostPayload): Future[Done]
  def getPostById(id: PostId, fromReadSide: Boolean = true): Future[Post]
  def getPostAnnotationById(id: PostId, fromReadSide: Boolean = true): Future[PostAnnotation]
  def getPostsById(ids: Set[PostId], fromReadSide: Boolean = true): Future[Map[PostId, Post]]
  def getPostAnnotationsById(ids: Set[PostId], fromReadSide: Boolean = true): Future[Map[PostId, PostAnnotation]]
  def findPosts(query: PostFindQuery): Future[FindResult]
  def addPostMedia(payload: AddPostMediaPayload): Future[Done]
  def removePostMedia(payload: RemovePostMediaPayload): Future[Done]
  def addPostDoc(payload: AddPostDocPayload): Future[Done]
  def updatePostDocName(payload: UpdatePostDocNamePayload): Future[Done]
  def removePostDoc(payload: RemovePostDocPayload): Future[Done]

  def viewPost(payload: ViewPostPayload): Future[Done]
  def likePost(payload: LikePostPayload): Future[Done]
  def getPostMetricById(id: PostId): Future[PostMetric]
  def getPostMetricsById(ids: Set[PostId]): Future[Map[PostId, PostMetric]]

}
