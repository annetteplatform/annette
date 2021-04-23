package biz.lobachev.annette.blogs.api

import akka.{Done, NotUsed}
import biz.lobachev.annette.blogs.api.blog._
import biz.lobachev.annette.blogs.api.category._
import biz.lobachev.annette.blogs.api.post._
import biz.lobachev.annette.blogs.api.post_metric._
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

import scala.collection.immutable.Map

trait BlogsServiceApi extends Service {

  def createCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category]
  def getCategoriesById(fromReadSide: Boolean = true): ServiceCall[Set[CategoryId], Map[CategoryId, Category]]
  def findCategories: ServiceCall[CategoryFindQuery, FindResult]

  def createBlog: ServiceCall[CreateBlogPayload, Done]
  def updateBlogName: ServiceCall[UpdateBlogNamePayload, Done]
  def updateBlogDescription: ServiceCall[UpdateBlogDescriptionPayload, Done]
  def updateBlogCategory: ServiceCall[UpdateBlogCategoryPayload, Done]
  def assignBlogTargetPrincipal: ServiceCall[AssignBlogTargetPrincipalPayload, Done]
  def unassignBlogTargetPrincipal: ServiceCall[UnassignBlogTargetPrincipalPayload, Done]
  def activateBlog: ServiceCall[ActivateBlogPayload, Done]
  def deactivateBlog: ServiceCall[DeactivateBlogPayload, Done]
  def deleteBlog: ServiceCall[DeleteBlogPayload, Done]
  def getBlogById(id: BlogId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Blog]
  def getBlogAnnotationById(id: BlogId, fromReadSide: Boolean = true): ServiceCall[NotUsed, BlogAnnotation]
  def getBlogsById(fromReadSide: Boolean = true): ServiceCall[Set[BlogId], Map[BlogId, Blog]]
  def getBlogAnnotationsById(fromReadSide: Boolean = true): ServiceCall[Set[BlogId], Map[BlogId, BlogAnnotation]]
  def findBlogs: ServiceCall[BlogFindQuery, FindResult]

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
  def getPostMetricById(id: PostId, fromReadSide: Boolean = true): ServiceCall[NotUsed, PostMetric]
  def getPostMetricsById(fromReadSide: Boolean = true): ServiceCall[Set[PostId], Map[PostId, PostMetric]]

  final override def descriptor = {
    import Service._
    named("blogs")
      .withCalls(
        pathCall("/api/blogs/v1/createCategory", createCategory),
        pathCall("/api/blogs/v1/updateCategory", updateCategory),
        pathCall("/api/blogs/v1/deleteCategory", deleteCategory),
        pathCall("/api/blogs/v1/getCategoryById/:id/:fromReadSide", getCategoryById _),
        pathCall("/api/blogs/v1/getCategoriesByIds/:fromReadSide", getCategoriesById _),
        pathCall("/api/blogs/v1/findCategories", findCategories),
        pathCall("/api/blogs/v1/createBlog", createBlog),
        pathCall("/api/blogs/v1/updateBlogName", updateBlogName),
        pathCall("/api/blogs/v1/updateBlogDescription", updateBlogDescription),
        pathCall("/api/blogs/v1/updateBlogCategory", updateBlogCategory),
        pathCall("/api/blogs/v1/assignBlogTargetPrincipal", assignBlogTargetPrincipal),
        pathCall("/api/blogs/v1/unassignBlogTargetPrincipal", unassignBlogTargetPrincipal),
        pathCall("/api/blogs/v1/activateBlog", activateBlog),
        pathCall("/api/blogs/v1/deactivateBlog", deactivateBlog),
        pathCall("/api/blogs/v1/deleteBlog", deleteBlog),
        pathCall("/api/blogs/v1/getBlogById/:id/:fromReadSide", getBlogById _),
        pathCall("/api/blogs/v1/getBlogAnnotationById/:id/:fromReadSide", getBlogAnnotationById _),
        pathCall("/api/blogs/v1/getBlogsByIds/:fromReadSide", getBlogsById _),
        pathCall("/api/blogs/v1/getBlogAnnotationsByIds/:fromReadSide", getBlogAnnotationsById _),
        pathCall("/api/blogs/v1/findBlogs", findBlogs),
        pathCall("/api/blogs/v1/createPost", createPost),
        pathCall("/api/blogs/v1/updatePostFeatured", updatePostFeatured),
        pathCall("/api/blogs/v1/updatePostAuthor", updatePostAuthor),
        pathCall("/api/blogs/v1/updatePostTitle", updatePostTitle),
        pathCall("/api/blogs/v1/updatePostIntro", updatePostIntro),
        pathCall("/api/blogs/v1/updatePostContent", updatePostContent),
        pathCall("/api/blogs/v1/updatePostPublicationTimestamp", updatePostPublicationTimestamp),
        pathCall("/api/blogs/v1/publishPost", publishPost),
        pathCall("/api/blogs/v1/unpublishPost", unpublishPost),
        pathCall("/api/blogs/v1/assignPostTargetPrincipal", assignPostTargetPrincipal),
        pathCall("/api/blogs/v1/unassignPostTargetPrincipal", unassignPostTargetPrincipal),
        pathCall("/api/blogs/v1/deletePost", deletePost),
        pathCall("/api/blogs/v1/getPostById/:id/:fromReadSide", getPostById _),
        pathCall("/api/blogs/v1/getPostAnnotationById/:id/:fromReadSide", getPostAnnotationById _),
        pathCall("/api/blogs/v1/getPostsByIds/:fromReadSide", getPostsById _),
        pathCall("/api/blogs/v1/getPostAnnotationsByIds/:fromReadSide", getPostAnnotationsById _),
        pathCall("/api/blogs/v1/findPosts", findPosts),
        pathCall("/api/blogs/v1/addPostMedia", addPostMedia),
        pathCall("/api/blogs/v1/removePostMedia", removePostMedia),
        pathCall("/api/blogs/v1/addPostDoc", addPostDoc),
        pathCall("/api/blogs/v1/updatePostDocName", updatePostDocName),
        pathCall("/api/blogs/v1/removePostDoc", removePostDoc),
        pathCall("/api/blogs/v1/viewPost", viewPost),
        pathCall("/api/blogs/v1/likePost", likePost),
        pathCall("/api/blogs/v1/getPostMetricById/:id/:fromReadSide", getPostMetricById _),
        pathCall("/api/blogs/v1/getPostMetricsByIds/:fromReadSide", getPostMetricsById _)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
