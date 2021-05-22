package biz.lobachev.annette.blogs.api
import akka.Done
import biz.lobachev.annette.blogs.api.blog._
import biz.lobachev.annette.blogs.api.category._
import biz.lobachev.annette.blogs.api.post._
import biz.lobachev.annette.core.model.elastic.FindResult

import scala.concurrent.Future

class BlogServiceImpl(api: BlogServiceApi) extends BlogService {
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

  override def createBlog(payload: CreateBlogPayload): Future[Done] =
    api.createBlog.invoke(payload)

  override def updateBlogName(payload: UpdateBlogNamePayload): Future[Done] =
    api.updateBlogName.invoke(payload)

  override def updateBlogDescription(payload: UpdateBlogDescriptionPayload): Future[Done] =
    api.updateBlogDescription.invoke(payload)

  override def updateBlogCategory(payload: UpdateBlogCategoryPayload): Future[Done] =
    api.updateBlogCategory.invoke(payload)

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

  override def getBlogAnnotationById(id: BlogId, fromReadSide: Boolean): Future[BlogAnnotation] =
    api.getBlogAnnotationById(id, fromReadSide).invoke()

  override def getBlogsById(ids: Set[BlogId], fromReadSide: Boolean): Future[Map[BlogId, Blog]] =
    api.getBlogsById(fromReadSide).invoke(ids)

  override def getBlogAnnotationsById(ids: Set[BlogId], fromReadSide: Boolean): Future[Map[BlogId, BlogAnnotation]] =
    api.getBlogAnnotationsById(fromReadSide).invoke(ids)

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
