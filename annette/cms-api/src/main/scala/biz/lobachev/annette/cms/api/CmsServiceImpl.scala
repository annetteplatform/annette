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

  override def getSpaceAnnotationById(id: SpaceId, fromReadSide: Boolean): Future[SpaceAnnotation] =
    api.getSpaceAnnotationById(id, fromReadSide).invoke()

  override def getSpacesById(ids: Set[SpaceId], fromReadSide: Boolean): Future[Map[SpaceId, Space]] =
    api.getSpacesById(fromReadSide).invoke(ids)

  override def getSpaceAnnotationsById(
    ids: Set[SpaceId],
    fromReadSide: Boolean
  ): Future[Map[SpaceId, SpaceAnnotation]] =
    api.getSpaceAnnotationsById(fromReadSide).invoke(ids)

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

  override def getPostMetricById(id: PostId): Future[PostMetric] =
    api.getPostMetricById(id).invoke()

  override def getPostMetricsById(ids: Set[PostId]): Future[Map[PostId, PostMetric]] =
    api.getPostMetricsById.invoke(ids)

}