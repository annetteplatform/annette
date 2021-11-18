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
import biz.lobachev.annette.cms.api.files.{FileDescriptor, RemoveFilePayload, RemoveFilesPayload, StoreFilePayload}
import biz.lobachev.annette.cms.impl.blogs.blog._
import biz.lobachev.annette.cms.impl.blogs.category.BlogCategoryEntityService
import biz.lobachev.annette.cms.impl.blogs.post._
import biz.lobachev.annette.cms.impl.files.FileEntityService
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class CmsServiceApiImpl(
  blogCategoryEntityService: BlogCategoryEntityService,
  blogEntityService: BlogEntityService,
  postEntityService: PostEntityService,
  fileEntityService: FileEntityService
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

  override def updatePostWidgetContent: ServiceCall[UpdatePostWidgetContentPayload, Done] =
    ServiceCall { payload =>
      postEntityService.updateWidgetContent(payload)
    }

  override def changePostWidgetContentOrder: ServiceCall[ChangePostWidgetContentOrderPayload, Done] =
    ServiceCall { payload =>
      postEntityService.changeWidgetContentOrder(payload)
    }

  override def deletePostWidgetContent: ServiceCall[DeletePostWidgetContentPayload, Done] =
    ServiceCall { payload =>
      postEntityService.deleteWidgetContent(payload)
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
      for {
        _ <- postEntityService.deletePost(payload)
        _ <- fileEntityService.removeFiles(
               RemoveFilesPayload(
                 objectId = s"post-${payload.id}",
                 updatedBy = payload.deletedBy
               )
             )
      } yield Done
    }

  override def getPostById(
    id: PostId,
    fromReadSide: Boolean = true,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[NotUsed, Post] =
    ServiceCall { _ =>
      postEntityService.getPostById(
        id,
        fromReadSide,
        withIntro.getOrElse(false),
        withContent.getOrElse(false),
        withTargets.getOrElse(false)
      )
    }

  override def getPostsById(
    fromReadSide: Boolean = true,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[Set[PostId], Seq[Post]] =
    ServiceCall { ids =>
      postEntityService.getPostsById(
        ids,
        fromReadSide,
        withIntro.getOrElse(false),
        withContent.getOrElse(false),
        withTargets.getOrElse(false)
      )
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

  override def storeFile: ServiceCall[StoreFilePayload, Done] =
    ServiceCall { payload =>
      fileEntityService.storeFile(payload)
    }

  override def removeFile: ServiceCall[RemoveFilePayload, Done] =
    ServiceCall { payload =>
      fileEntityService.removeFile(payload)
    }

  override def removeFiles: ServiceCall[RemoveFilesPayload, Done] =
    ServiceCall { payload =>
      fileEntityService.removeFiles(payload)
    }

  override def getFiles(objectId: String): ServiceCall[NotUsed, Seq[FileDescriptor]] =
    ServiceCall { _ =>
      fileEntityService.getFiles(objectId)
    }
}
