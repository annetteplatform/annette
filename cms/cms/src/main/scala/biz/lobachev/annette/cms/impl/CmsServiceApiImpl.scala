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

import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.cms.api._
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.impl.blogs.category.BlogCategoryEntityService
import biz.lobachev.annette.cms.impl.blogs.blog._
import biz.lobachev.annette.cms.impl.blogs.post._
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext}
import scala.concurrent.duration._

class CmsServiceApiImpl(
  blogCategoryEntityService: BlogCategoryEntityService,
  blogEntityService: BlogEntityService,
  postEntityService: PostEntityService
)(implicit
  ec: ExecutionContext
) extends CmsServiceApi {

  implicit val timeout = Timeout(50.seconds)

  val log = LoggerFactory.getLogger(this.getClass)

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

  override def getBlogCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      blogCategoryEntityService.getCategoryById(id, fromReadSide)
    }

  override def getBlogCategoriesById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      blogCategoryEntityService.getCategoriesById(ids, fromReadSide)
    }

  override def findBlogCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      blogCategoryEntityService.findCategories(query)
    }

  override def createBlog: ServiceCall[CreateBlogPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.createBlog(payload)
    }

  override def updateBlogName: ServiceCall[UpdateBlogNamePayload, Done] =
    ServiceCall { payload =>
      blogEntityService.updateBlogName(payload)
    }

  override def updateBlogDescription: ServiceCall[UpdateBlogDescriptionPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.updateBlogDescription(payload)
    }

  override def updateBlogCategoryId: ServiceCall[UpdateBlogCategoryPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.updateBlogCategoryId(payload)
    }

  override def assignBlogTargetPrincipal: ServiceCall[AssignBlogTargetPrincipalPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.assignBlogTargetPrincipal(payload)
    }

  override def unassignBlogTargetPrincipal: ServiceCall[UnassignBlogTargetPrincipalPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.unassignBlogTargetPrincipal(payload)
    }

  override def activateBlog: ServiceCall[ActivateBlogPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.activateBlog(payload)
    }

  override def deactivateBlog: ServiceCall[DeactivateBlogPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.deactivateBlog(payload)
    }

  override def deleteBlog: ServiceCall[DeleteBlogPayload, Done] =
    ServiceCall { payload =>
      // TODO: validate if posts exist
      blogEntityService.deleteBlog(payload)
    }

  override def getBlogById(id: BlogId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Blog] =
    ServiceCall { _ =>
      blogEntityService.getBlogById(id, fromReadSide)
    }

  override def getBlogsById(fromReadSide: Boolean = true): ServiceCall[Set[BlogId], Seq[Blog]] =
    ServiceCall { ids =>
      blogEntityService.getBlogsById(ids, fromReadSide)
    }

  override def getBlogViews: ServiceCall[GetBlogViewsPayload, Seq[BlogView]] =
    ServiceCall { payload =>
      blogEntityService.getBlogViews(payload)
    }

  override def canAccessToBlog: ServiceCall[CanAccessToBlogPayload, Boolean] =
    ServiceCall { payload =>
      blogEntityService.canAccessToBlog(payload)
    }

  override def findBlogs: ServiceCall[BlogFindQuery, FindResult] =
    ServiceCall { query =>
      blogEntityService.findBlogs(query)
    }

  override def createPost: ServiceCall[CreatePostPayload, Done] =
    ServiceCall { payload =>
      for {
        // validate if blog exist
        // TODO: create isBlogExist method
        blog <- blogEntityService.getBlogById(payload.blogId, false)
        _    <- postEntityService
                  .createPost(payload, blog.targets)

      } yield Done
    }

  override def updatePostFeatured: ServiceCall[UpdatePostFeaturedPayload, Done] =
    ServiceCall { payload =>
      postEntityService.updatePostFeatured(payload)
    }

  override def updatePostAuthor: ServiceCall[UpdatePostAuthorPayload, Done] =
    ServiceCall { payload =>
      postEntityService.updatePostAuthor(payload)
    }

  override def updatePostTitle: ServiceCall[UpdatePostTitlePayload, Done] =
    ServiceCall { payload =>
      postEntityService.updatePostTitle(payload)
    }

  override def updatePostIntro: ServiceCall[UpdatePostIntroPayload, Done] =
    ServiceCall { payload =>
      postEntityService.updatePostIntro(payload)
    }

  override def updatePostContent: ServiceCall[UpdatePostContentPayload, Done] =
    ServiceCall { payload =>
      postEntityService.updatePostContent(payload)
    }

  override def updatePostPublicationTimestamp: ServiceCall[UpdatePostPublicationTimestampPayload, Done] =
    ServiceCall { payload =>
      postEntityService.updatePostPublicationTimestamp(payload)
    }

  override def publishPost: ServiceCall[PublishPostPayload, Done] =
    ServiceCall { payload =>
      postEntityService.publishPost(payload)
    }

  override def unpublishPost: ServiceCall[UnpublishPostPayload, Done] =
    ServiceCall { payload =>
      postEntityService.unpublishPost(payload)
    }

  override def assignPostTargetPrincipal: ServiceCall[AssignPostTargetPrincipalPayload, Done] =
    ServiceCall { payload =>
      postEntityService.assignPostTargetPrincipal(payload)
    }

  override def unassignPostTargetPrincipal: ServiceCall[UnassignPostTargetPrincipalPayload, Done] =
    ServiceCall { payload =>
      postEntityService.unassignPostTargetPrincipal(payload)
    }

  override def deletePost: ServiceCall[DeletePostPayload, Done] =
    ServiceCall { payload =>
      postEntityService.deletePost(payload)
    }

  override def getPostById(id: PostId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Post] =
    ServiceCall { _ =>
      postEntityService.getPostById(id, fromReadSide)
    }

  override def getPostAnnotationById(id: PostId, fromReadSide: Boolean = true): ServiceCall[NotUsed, PostAnnotation] =
    ServiceCall { _ =>
      postEntityService.getPostAnnotationById(id, fromReadSide)
    }

  override def getPostsById(fromReadSide: Boolean = true): ServiceCall[Set[PostId], Seq[Post]] =
    ServiceCall { ids =>
      postEntityService.getPostsById(ids, fromReadSide)
    }

  override def getPostAnnotationsById(fromReadSide: Boolean = true): ServiceCall[Set[PostId], Seq[PostAnnotation]] =
    ServiceCall { ids =>
      postEntityService.getPostAnnotationsById(ids, fromReadSide)
    }

  override def getPostViews: ServiceCall[GetPostViewsPayload, Seq[PostView]] =
    ServiceCall { payload =>
      postEntityService.getPostViews(payload)
    }

  override def canAccessToPost: ServiceCall[CanAccessToPostPayload, Boolean] =
    ServiceCall { payload =>
      postEntityService.canAccessToPost(payload)
    }

  override def findPosts: ServiceCall[PostFindQuery, FindResult] =
    ServiceCall { query =>
      postEntityService.findPosts(query)
    }

  override def addPostMedia: ServiceCall[AddPostMediaPayload, Done] =
    ServiceCall { payload =>
      postEntityService.addPostMedia(payload)
    }

  override def removePostMedia: ServiceCall[RemovePostMediaPayload, Done] =
    ServiceCall { payload =>
      postEntityService.removePostMedia(payload)
    }

  override def addPostDoc: ServiceCall[AddPostDocPayload, Done] =
    ServiceCall { payload =>
      postEntityService.addPostDoc(payload)
    }

  override def updatePostDocName: ServiceCall[UpdatePostDocNamePayload, Done] =
    ServiceCall { payload =>
      postEntityService.updatePostDocName(payload)
    }

  override def removePostDoc: ServiceCall[RemovePostDocPayload, Done] =
    ServiceCall { payload =>
      postEntityService.removePostDoc(payload)
    }

  override def viewPost: ServiceCall[ViewPostPayload, Done] =
    ServiceCall { payload =>
      postEntityService.viewPost(payload)
    }

  override def likePost: ServiceCall[LikePostPayload, Done] =
    ServiceCall { payload =>
      postEntityService.likePost(payload)
    }

  override def unlikePost: ServiceCall[UnlikePostPayload, Done] =
    ServiceCall { payload =>
      postEntityService.unlikePost(payload)
    }

  override def getPostMetricById: ServiceCall[GetPostMetricPayload, PostMetric] =
    ServiceCall { payload =>
      postEntityService.getPostMetricById(payload)
    }

  override def getPostMetricsById: ServiceCall[GetPostMetricsPayload, Seq[PostMetric]] =
    ServiceCall { payload =>
      postEntityService.getPostMetricsById(payload)
    }

  // ************************** CMS Pages **************************

//  override def createCategory: ServiceCall[CreateCategoryPayload, Done] =
//    ServiceCall { payload =>
//      categoryEntityService.createCategory(payload)
//    }
//
//  override def updateCategory: ServiceCall[UpdateCategoryPayload, Done] =
//    ServiceCall { payload =>
//      categoryEntityService.updateCategory(payload)
//    }
//
//  override def deleteCategory: ServiceCall[DeleteCategoryPayload, Done] =
//    ServiceCall { payload =>
//      categoryEntityService.deleteCategory(payload)
//    }
//
//  override def getCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category] =
//    ServiceCall { _ =>
//      categoryEntityService.getCategoryById(id, fromReadSide)
//    }
//
//  override def getCategoriesById(
//                                  fromReadSide: Boolean = true
//                                ): ServiceCall[Set[CategoryId], Seq[Category]] =
//    ServiceCall { ids =>
//      categoryEntityService.getCategoriesById(ids, fromReadSide)
//    }
//
//  override def findCategories: ServiceCall[CategoryFindQuery, FindResult] =
//    ServiceCall { query =>
//      categoryEntityService.findCategories(query)
//    }
//
//  override def createSpace: ServiceCall[CreateSpacePayload, Done] =
//    ServiceCall { payload =>
//      for {
//        _ <- spaceEntityService.createSpace(payload)
//        _ <- if (payload.spaceType == SpaceType.Wiki) hierarchyEntityService.createSpace(payload)
//        else Future.successful(Done)
//      } yield Done
//    }
//
//  override def updateSpaceName: ServiceCall[UpdateSpaceNamePayload, Done] =
//    ServiceCall { payload =>
//      spaceEntityService.updateSpaceName(payload)
//    }
//
//  override def updateSpaceDescription: ServiceCall[UpdateSpaceDescriptionPayload, Done] =
//    ServiceCall { payload =>
//      spaceEntityService.updateSpaceDescription(payload)
//    }
//
//  override def updateSpaceCategory: ServiceCall[UpdateSpaceCategoryPayload, Done] =
//    ServiceCall { payload =>
//      spaceEntityService.updateSpaceCategory(payload)
//    }
//
//  override def assignSpaceTargetPrincipal: ServiceCall[AssignSpaceTargetPrincipalPayload, Done] =
//    ServiceCall { payload =>
//      spaceEntityService.assignSpaceTargetPrincipal(payload)
//    }
//
//  override def unassignSpaceTargetPrincipal: ServiceCall[UnassignSpaceTargetPrincipalPayload, Done] =
//    ServiceCall { payload =>
//      spaceEntityService.unassignSpaceTargetPrincipal(payload)
//    }
//
//  override def activateSpace: ServiceCall[ActivateSpacePayload, Done] =
//    ServiceCall { payload =>
//      spaceEntityService.activateSpace(payload)
//    }
//
//  override def deactivateSpace: ServiceCall[DeactivateSpacePayload, Done] =
//    ServiceCall { payload =>
//      spaceEntityService.deactivateSpace(payload)
//    }
//
//  override def deleteSpace: ServiceCall[DeleteSpacePayload, Done] =
//    ServiceCall { payload =>
//      for {
//        space <- spaceEntityService.getSpaceById(payload.id, false)
//        _     <- if (space.spaceType == SpaceType.Wiki) hierarchyEntityService.deleteSpace(payload)
//        else Future.successful(Done)
//        _     <- spaceEntityService.deleteSpace(payload)
//      } yield Done
//    }
//
//  override def getSpaceById(id: SpaceId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Space] =
//    ServiceCall { _ =>
//      spaceEntityService.getSpaceById(id, fromReadSide)
//    }
//
//  override def getSpacesById(fromReadSide: Boolean = true): ServiceCall[Set[SpaceId], Seq[Space]] =
//    ServiceCall { ids =>
//      spaceEntityService.getSpacesById(ids, fromReadSide)
//    }
//
//  override def getSpaceViews: ServiceCall[GetSpaceViewsPayload, Seq[SpaceView]] =
//    ServiceCall { payload =>
//      spaceEntityService.getSpaceViews(payload)
//    }
//
//  override def canAccessToSpace: ServiceCall[CanAccessToSpacePayload, Boolean] =
//    ServiceCall { payload =>
//      spaceEntityService.canAccessToSpace(payload)
//    }
//
//  override def findSpaces: ServiceCall[SpaceFindQuery, FindResult] =
//    ServiceCall { query =>
//      spaceEntityService.findSpaces(query)
//    }
//
//  override def createPost: ServiceCall[CreatePostPayload, Done] =
//    ServiceCall { payload =>
//      for {
//        // validate if space exist
//        space <- spaceEntityService.getSpaceById(payload.spaceId, false)
//        // if space is wiki assign post to hierarchy
//        _     <- if (space.spaceType == SpaceType.Wiki)
//          hierarchyEntityService.addPost(
//            spaceId = payload.spaceId,
//            postId = payload.id,
//            parent = payload.parent,
//            updatedBy = payload.createdBy
//          )
//        else Future.successful(Done)
//        // create post with targets from space
//        _     <- postEntityService
//          .createPost(payload, space.targets)
//          .recoverWith { th =>
//            if (space.spaceType == SpaceType.Wiki)
//            // remove post from hierarchy if create post failed
//              hierarchyEntityService
//                .removePost(payload.spaceId, payload.id, payload.createdBy)
//                .map(_ => throw th)
//            else Future.failed(th)
//          }
//      } yield Done
//    }
//
//  override def updatePostFeatured: ServiceCall[UpdatePostFeaturedPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.updatePostFeatured(payload)
//    }
//
//  override def updatePostAuthor: ServiceCall[UpdatePostAuthorPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.updatePostAuthor(payload)
//    }
//
//  override def updatePostTitle: ServiceCall[UpdatePostTitlePayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.updatePostTitle(payload)
//    }
//
//  override def updatePostIntro: ServiceCall[UpdatePostIntroPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.updatePostIntro(payload)
//    }
//
//  override def updatePostContent: ServiceCall[UpdatePostContentPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.updatePostContent(payload)
//    }
//
//  override def updatePostPublicationTimestamp: ServiceCall[UpdatePostPublicationTimestampPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.updatePostPublicationTimestamp(payload)
//    }
//
//  override def publishPost: ServiceCall[PublishPostPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.publishPost(payload)
//    }
//
//  override def unpublishPost: ServiceCall[UnpublishPostPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.unpublishPost(payload)
//    }
//
//  override def assignPostTargetPrincipal: ServiceCall[AssignPostTargetPrincipalPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.assignPostTargetPrincipal(payload)
//    }
//
//  override def unassignPostTargetPrincipal: ServiceCall[UnassignPostTargetPrincipalPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.unassignPostTargetPrincipal(payload)
//    }
//
//  override def deletePost: ServiceCall[DeletePostPayload, Done] =
//    ServiceCall { payload =>
//      for {
//        // get post's space
//        post  <- postEntityService.getPostAnnotationById(payload.id, false)
//        space <- spaceEntityService.getSpaceById(post.spaceId, false)
//        // if space is wiki remove post from hierarchy
//        _     <- if (space.spaceType == SpaceType.Wiki)
//          hierarchyEntityService.removePost(post.spaceId, payload.id, payload.deletedBy)
//        else Future.successful(Done)
//        _     <- postEntityService.deletePost(payload)
//
//      } yield Done
//    }
//
//  override def getPostById(id: PostId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Post] =
//    ServiceCall { _ =>
//      postEntityService.getPostById(id, fromReadSide)
//    }
//
//  override def getPostAnnotationById(id: PostId, fromReadSide: Boolean = true): ServiceCall[NotUsed, PostAnnotation] =
//    ServiceCall { _ =>
//      postEntityService.getPostAnnotationById(id, fromReadSide)
//    }
//
//  override def getPostsById(fromReadSide: Boolean = true): ServiceCall[Set[PostId], Seq[Post]] =
//    ServiceCall { ids =>
//      postEntityService.getPostsById(ids, fromReadSide)
//    }
//
//  override def getPostAnnotationsById(fromReadSide: Boolean = true): ServiceCall[Set[PostId], Seq[PostAnnotation]] =
//    ServiceCall { ids =>
//      postEntityService.getPostAnnotationsById(ids, fromReadSide)
//    }
//
//  override def getPostViews: ServiceCall[GetPostViewsPayload, Seq[PostView]] =
//    ServiceCall { payload =>
//      postEntityService.getPostViews(payload)
//    }
//
//  override def canAccessToPost: ServiceCall[CanAccessToPostPayload, Boolean] =
//    ServiceCall { payload =>
//      postEntityService.canAccessToPost(payload)
//    }
//
//  override def findPosts: ServiceCall[PostFindQuery, FindResult] =
//    ServiceCall { query =>
//      postEntityService.findPosts(query)
//    }
//
//  override def addPostMedia: ServiceCall[AddPostMediaPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.addPostMedia(payload)
//    }
//
//  override def removePostMedia: ServiceCall[RemovePostMediaPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.removePostMedia(payload)
//    }
//
//  override def addPostDoc: ServiceCall[AddPostDocPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.addPostDoc(payload)
//    }
//
//  override def updatePostDocName: ServiceCall[UpdatePostDocNamePayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.updatePostDocName(payload)
//    }
//
//  override def removePostDoc: ServiceCall[RemovePostDocPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.removePostDoc(payload)
//    }
//
//  override def viewPost: ServiceCall[ViewPostPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.viewPost(payload)
//    }
//
//  override def likePost: ServiceCall[LikePostPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.likePost(payload)
//    }
//
//  override def unlikePost: ServiceCall[UnlikePostPayload, Done] =
//    ServiceCall { payload =>
//      postEntityService.unlikePost(payload)
//    }
//
//  override def getPostMetricById: ServiceCall[GetPostMetricPayload, PostMetric] =
//    ServiceCall { payload =>
//      postEntityService.getPostMetricById(payload)
//    }
//
//  override def getPostMetricsById: ServiceCall[GetPostMetricsPayload, Seq[PostMetric]] =
//    ServiceCall { payload =>
//      postEntityService.getPostMetricsById(payload)
//    }
//
//  override def movePost: ServiceCall[MovePostPayload, Done] =
//    ServiceCall { payload =>
//      for {
//        post <- postEntityService.getPostAnnotationById(payload.postId, false)
//        _    <- hierarchyEntityService.movePost(
//          spaceId = post.spaceId,
//          postId = payload.postId,
//          newParent = payload.newParentId,
//          newPosition = payload.order,
//          updatedBy = payload.updatedBy
//        )
//      } yield Done
//    }
//
//  override def getWikiHierarchyById(spaceId: SpaceId, fromReadSide: Boolean): ServiceCall[NotUsed, WikiHierarchy] =
//    ServiceCall { _ =>
//      hierarchyEntityService.getHierarchyById(spaceId, fromReadSide)
//    }
}
