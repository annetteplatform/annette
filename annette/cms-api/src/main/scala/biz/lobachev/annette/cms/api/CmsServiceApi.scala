package biz.lobachev.annette.cms.api

import akka.{Done, NotUsed}
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
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

import scala.collection.immutable.Map

trait CmsServiceApi extends Service {

  def createCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category]
  def getCategoriesById(fromReadSide: Boolean = true): ServiceCall[Set[CategoryId], Map[CategoryId, Category]]
  def findCategories: ServiceCall[CategoryFindQuery, FindResult]

  def createSpace: ServiceCall[CreateSpacePayload, Done]
  def updateSpaceName: ServiceCall[UpdateSpaceNamePayload, Done]
  def updateSpaceDescription: ServiceCall[UpdateSpaceDescriptionPayload, Done]
  def updateSpaceCategory: ServiceCall[UpdateSpaceCategoryPayload, Done]
  def assignSpaceTargetPrincipal: ServiceCall[AssignSpaceTargetPrincipalPayload, Done]
  def unassignSpaceTargetPrincipal: ServiceCall[UnassignSpaceTargetPrincipalPayload, Done]
  def activateSpace: ServiceCall[ActivateSpacePayload, Done]
  def deactivateSpace: ServiceCall[DeactivateSpacePayload, Done]
  def deleteSpace: ServiceCall[DeleteSpacePayload, Done]
  def getSpaceById(id: SpaceId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Space]
  def getSpaceAnnotationById(id: SpaceId, fromReadSide: Boolean = true): ServiceCall[NotUsed, SpaceAnnotation]
  def getSpacesById(fromReadSide: Boolean = true): ServiceCall[Set[SpaceId], Map[SpaceId, Space]]
  def getSpaceAnnotationsById(fromReadSide: Boolean = true): ServiceCall[Set[SpaceId], Map[SpaceId, SpaceAnnotation]]
  def findSpaces: ServiceCall[SpaceFindQuery, FindResult]

  def createPost: ServiceCall[CreatePostPayload, Done]
  def updatePostFeatured: ServiceCall[UpdatePostFeaturedPayload, Done]
  def updatePostAuthor: ServiceCall[UpdatePostAuthorPayload, Done]
  def updatePostTitle: ServiceCall[UpdatePostTitlePayload, Done]
  def updatePostIntro: ServiceCall[UpdatePostIntroPayload, Done]
  def updatePostContent: ServiceCall[UpdatePostContentPayload, Done]
  def updatePostPublicationTimestamp: ServiceCall[UpdatePostPublicationTimestampPayload, Done]
  def publishPost: ServiceCall[PublishPostPayload, Done]
  def unpublishPost: ServiceCall[UnpublishPostPayload, Done]
  def assignPostTargetPrincipal: ServiceCall[AssignPostTargetPrincipalPayload, Done]
  def unassignPostTargetPrincipal: ServiceCall[UnassignPostTargetPrincipalPayload, Done]
  def deletePost: ServiceCall[DeletePostPayload, Done]
  def getPostById(id: PostId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Post]
  def getPostAnnotationById(id: PostId, fromReadSide: Boolean = true): ServiceCall[NotUsed, PostAnnotation]
  def getPostsById(fromReadSide: Boolean = true): ServiceCall[Set[PostId], Map[PostId, Post]]
  def getPostAnnotationsById(fromReadSide: Boolean = true): ServiceCall[Set[PostId], Map[PostId, PostAnnotation]]
  def findPosts: ServiceCall[PostFindQuery, FindResult]
  def addPostMedia: ServiceCall[AddPostMediaPayload, Done]
  def removePostMedia: ServiceCall[RemovePostMediaPayload, Done]
  def addPostDoc: ServiceCall[AddPostDocPayload, Done]
  def updatePostDocName: ServiceCall[UpdatePostDocNamePayload, Done]
  def removePostDoc: ServiceCall[RemovePostDocPayload, Done]

  def viewPost: ServiceCall[ViewPostPayload, Done]
  def likePost: ServiceCall[LikePostPayload, Done]
  def getPostMetricById(id: PostId): ServiceCall[NotUsed, PostMetric]
  def getPostMetricsById: ServiceCall[Set[PostId], Map[PostId, PostMetric]]

  final override def descriptor = {
    import Service._
    named("cms")
      .withCalls(
        pathCall("/api/cms/v1/createCategory", createCategory),
        pathCall("/api/cms/v1/updateCategory", updateCategory),
        pathCall("/api/cms/v1/deleteCategory", deleteCategory),
        pathCall("/api/cms/v1/getCategoryById/:id/:fromReadSide", getCategoryById _),
        pathCall("/api/cms/v1/getCategoriesById/:fromReadSide", getCategoriesById _),
        pathCall("/api/cms/v1/findCategories", findCategories),
        pathCall("/api/cms/v1/createSpace", createSpace),
        pathCall("/api/cms/v1/updateSpaceName", updateSpaceName),
        pathCall("/api/cms/v1/updateSpaceDescription", updateSpaceDescription),
        pathCall("/api/cms/v1/updateSpaceCategory", updateSpaceCategory),
        pathCall("/api/cms/v1/assignSpaceTargetPrincipal", assignSpaceTargetPrincipal),
        pathCall("/api/cms/v1/unassignSpaceTargetPrincipal", unassignSpaceTargetPrincipal),
        pathCall("/api/cms/v1/activateSpace", activateSpace),
        pathCall("/api/cms/v1/deactivateSpace", deactivateSpace),
        pathCall("/api/cms/v1/deleteSpace", deleteSpace),
        pathCall("/api/cms/v1/getSpaceById/:id/:fromReadSide", getSpaceById _),
        pathCall("/api/cms/v1/getSpaceAnnotationById/:id/:fromReadSide", getSpaceAnnotationById _),
        pathCall("/api/cms/v1/getSpacesByIds/:fromReadSide", getSpacesById _),
        pathCall("/api/cms/v1/getSpaceAnnotationsById/:fromReadSide", getSpaceAnnotationsById _),
        pathCall("/api/cms/v1/findSpaces", findSpaces),
        pathCall("/api/cms/v1/createPost", createPost),
        pathCall("/api/cms/v1/updatePostFeatured", updatePostFeatured),
        pathCall("/api/cms/v1/updatePostAuthor", updatePostAuthor),
        pathCall("/api/cms/v1/updatePostTitle", updatePostTitle),
        pathCall("/api/cms/v1/updatePostIntro", updatePostIntro),
        pathCall("/api/cms/v1/updatePostContent", updatePostContent),
        pathCall("/api/cms/v1/updatePostPublicationTimestamp", updatePostPublicationTimestamp),
        pathCall("/api/cms/v1/publishPost", publishPost),
        pathCall("/api/cms/v1/unpublishPost", unpublishPost),
        pathCall("/api/cms/v1/assignPostTargetPrincipal", assignPostTargetPrincipal),
        pathCall("/api/cms/v1/unassignPostTargetPrincipal", unassignPostTargetPrincipal),
        pathCall("/api/cms/v1/deletePost", deletePost),
        pathCall("/api/cms/v1/getPostById/:id/:fromReadSide", getPostById _),
        pathCall("/api/cms/v1/getPostAnnotationById/:id/:fromReadSide", getPostAnnotationById _),
        pathCall("/api/cms/v1/getPostsById/:fromReadSide", getPostsById _),
        pathCall("/api/cms/v1/getPostAnnotationsById/:fromReadSide", getPostAnnotationsById _),
        pathCall("/api/cms/v1/findPosts", findPosts),
        pathCall("/api/cms/v1/addPostMedia", addPostMedia),
        pathCall("/api/cms/v1/removePostMedia", removePostMedia),
        pathCall("/api/cms/v1/addPostDoc", addPostDoc),
        pathCall("/api/cms/v1/updatePostDocName", updatePostDocName),
        pathCall("/api/cms/v1/removePostDoc", removePostDoc),
        pathCall("/api/cms/v1/viewPost", viewPost),
        pathCall("/api/cms/v1/likePost", likePost),
        pathCall("/api/cms/v1/getPostMetricById/:id", getPostMetricById _),
        pathCall("/api/cms/v1/getPostMetricsById", getPostMetricsById)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
