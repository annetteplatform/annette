package biz.lobachev.annette.blogs.api

import akka.Done
import biz.lobachev.annette.blogs.api.blog._
import biz.lobachev.annette.blogs.api.category._
import biz.lobachev.annette.blogs.api.post._
import biz.lobachev.annette.core.model.elastic.FindResult

import scala.collection.immutable.Map
import scala.concurrent.Future

trait BlogService {

  def createCategory(payload: CreateCategoryPayload): Future[Done]
  def updateCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteCategory(payload: DeleteCategoryPayload): Future[Done]
  def getCategoryById(id: CategoryId, fromReadSide: Boolean = true): Future[Category]
  def getCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean = true): Future[Map[CategoryId, Category]]
  def findCategories(payload: CategoryFindQuery): Future[FindResult]

  def createBlog(payload: CreateBlogPayload): Future[Done]
  def updateBlogName(payload: UpdateBlogNamePayload): Future[Done]
  def updateBlogDescription(payload: UpdateBlogDescriptionPayload): Future[Done]
  def updateBlogCategory(payload: UpdateBlogCategoryPayload): Future[Done]
  def assignBlogTargetPrincipal(payload: AssignBlogTargetPrincipalPayload): Future[Done]
  def unassignBlogTargetPrincipal(payload: UnassignBlogTargetPrincipalPayload): Future[Done]
  def activateBlog(payload: ActivateBlogPayload): Future[Done]
  def deactivateBlog(payload: DeactivateBlogPayload): Future[Done]
  def deleteBlog(payload: DeleteBlogPayload): Future[Done]
  def getBlogById(id: BlogId, fromReadSide: Boolean = true): Future[Blog]
  def getBlogAnnotationById(id: BlogId, fromReadSide: Boolean = true): Future[BlogAnnotation]
  def getBlogsById(ids: Set[BlogId], fromReadSide: Boolean = true): Future[Map[BlogId, Blog]]
  def getBlogAnnotationsById(ids: Set[BlogId], fromReadSide: Boolean = true): Future[Map[BlogId, BlogAnnotation]]
  def findBlogs(payload: BlogFindQuery): Future[FindResult]

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
