package biz.lobachev.annette.blogs.impl

import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.blogs.api._
import biz.lobachev.annette.blogs.api.blog._
import biz.lobachev.annette.blogs.api.category._
import biz.lobachev.annette.blogs.api.post._
import biz.lobachev.annette.blogs.api.post_metric._
import biz.lobachev.annette.blogs.impl.blog._
import biz.lobachev.annette.blogs.impl.category._
import biz.lobachev.annette.blogs.impl.post._
import biz.lobachev.annette.blogs.impl.post_metric._
import biz.lobachev.annette.core.model.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

class BlogsServiceApiImpl(
  categoryEntityService: CategoryEntityService,
  blogEntityService: BlogEntityService,
  postEntityService: PostEntityService,
  postMetricEntityService: PostMetricEntityService
) extends BlogsServiceApi {

  implicit val timeout = Timeout(50.seconds)

  val log = LoggerFactory.getLogger(this.getClass)

  override def createCategory: ServiceCall[CreateCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.createCategory(payload)
    }

  override def updateCategory: ServiceCall[UpdateCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.updateCategory(payload)
    }

  override def deleteCategory: ServiceCall[DeleteCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.deleteCategory(payload)
    }

  override def getCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      categoryEntityService.getCategoryById(id, fromReadSide)
    }

  override def getCategoriesById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[CategoryId], Map[CategoryId, Category]] =
    ServiceCall { ids =>
      categoryEntityService.getCategoriesById(ids, fromReadSide)
    }

  override def findCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      categoryEntityService.findCategories(query)
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

  override def updateBlogCategory: ServiceCall[UpdateBlogCategoryPayload, Done] =
    ServiceCall { payload =>
      blogEntityService.updateBlogCategory(payload)
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
      blogEntityService.deleteBlog(payload)
    }

  override def getBlogById(id: BlogId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Blog] =
    ServiceCall { _ =>
      blogEntityService.getBlogById(id, fromReadSide)
    }

  override def getBlogAnnotationById(id: BlogId, fromReadSide: Boolean = true): ServiceCall[NotUsed, BlogAnnotation] =
    ServiceCall { _ =>
      blogEntityService.getBlogAnnotationById(id, fromReadSide)
    }

  override def getBlogsById(fromReadSide: Boolean = true): ServiceCall[Set[BlogId], Map[BlogId, Blog]] =
    ServiceCall { ids =>
      blogEntityService.getBlogsById(ids, fromReadSide)
    }

  override def getBlogAnnotationsById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[BlogId], Map[BlogId, BlogAnnotation]] =
    ServiceCall { ids =>
      blogEntityService.getBlogAnnotationsById(ids, fromReadSide)
    }

  override def findBlogs: ServiceCall[BlogFindQuery, FindResult] =
    ServiceCall { query =>
      blogEntityService.findBlogs(query)
    }

  override def createPost: ServiceCall[CreatePostPayload, Done] =
    ServiceCall { payload =>
      postEntityService.createPost(payload)
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

  override def getPostsById(fromReadSide: Boolean = true): ServiceCall[Set[PostId], Map[PostId, Post]] =
    ServiceCall { ids =>
      postEntityService.getPostsById(ids, fromReadSide)
    }

  override def getPostAnnotationsById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[PostId], Map[PostId, PostAnnotation]] =
    ServiceCall { ids =>
      postEntityService.getPostAnnotationsById(ids, fromReadSide)
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
      postMetricEntityService.viewPost(payload)
    }

  override def likePost: ServiceCall[LikePostPayload, Done] =
    ServiceCall { payload =>
      postMetricEntityService.likePost(payload)
    }

  override def getPostMetricById(id: PostId, fromReadSide: Boolean = true): ServiceCall[NotUsed, PostMetric] =
    ServiceCall { _ =>
      postMetricEntityService.getPostMetricById(id, fromReadSide)
    }

  override def getPostMetricsById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[PostId], Map[PostId, PostMetric]] =
    ServiceCall { ids =>
      postMetricEntityService.getPostMetricsById(ids, fromReadSide)
    }

}
