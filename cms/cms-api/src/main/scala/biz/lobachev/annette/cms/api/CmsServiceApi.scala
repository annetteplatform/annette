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

import akka.{Done, NotUsed}
import biz.lobachev.annette.cms.api.blogs.blog.{BlogView, _}
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.files._
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait CmsServiceApi extends Service {

  // ************************** CMS Files **************************

  def storeFile: ServiceCall[StoreFilePayload, Done]
  def removeFile: ServiceCall[RemoveFilePayload, Done]
  def removeFiles: ServiceCall[RemoveFilesPayload, Done]
  def getFiles(objectId: String): ServiceCall[NotUsed, Seq[FileDescriptor]]

  // ************************** CMS Blogs **************************

  def createBlogCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateBlogCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteBlogCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getBlogCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category]
  def getBlogCategoriesById(fromReadSide: Boolean = true): ServiceCall[Set[CategoryId], Seq[Category]]
  def findBlogCategories: ServiceCall[CategoryFindQuery, FindResult]

  def createBlog: ServiceCall[CreateBlogPayload, Done]
  def updateBlogName: ServiceCall[UpdateBlogNamePayload, Done]
  def updateBlogDescription: ServiceCall[UpdateBlogDescriptionPayload, Done]
  def updateBlogCategoryId: ServiceCall[UpdateBlogCategoryPayload, Done]
  def assignBlogTargetPrincipal: ServiceCall[AssignBlogTargetPrincipalPayload, Done]
  def unassignBlogTargetPrincipal: ServiceCall[UnassignBlogTargetPrincipalPayload, Done]
  def activateBlog: ServiceCall[ActivateBlogPayload, Done]
  def deactivateBlog: ServiceCall[DeactivateBlogPayload, Done]
  def deleteBlog: ServiceCall[DeleteBlogPayload, Done]
  def getBlogById(id: BlogId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Blog]
  def getBlogsById(fromReadSide: Boolean = true): ServiceCall[Set[BlogId], Seq[Blog]]
  def getBlogViews: ServiceCall[GetBlogViewsPayload, Seq[BlogView]]
  def canAccessToBlog: ServiceCall[CanAccessToBlogPayload, Boolean]
  def findBlogs: ServiceCall[BlogFindQuery, FindResult]

  def createPost: ServiceCall[CreatePostPayload, Done]
  def updatePostFeatured: ServiceCall[UpdatePostFeaturedPayload, Done]
  def updatePostAuthor: ServiceCall[UpdatePostAuthorPayload, Done]
  def updatePostTitle: ServiceCall[UpdatePostTitlePayload, Done]
  def updatePostWidgetContent: ServiceCall[UpdatePostWidgetContentPayload, Done]
  def changePostWidgetContentOrder: ServiceCall[ChangePostWidgetContentOrderPayload, Done]
  def deletePostWidgetContent: ServiceCall[DeletePostWidgetContentPayload, Done]
  def updatePostPublicationTimestamp: ServiceCall[UpdatePostPublicationTimestampPayload, Done]
  def publishPost: ServiceCall[PublishPostPayload, Done]
  def unpublishPost: ServiceCall[UnpublishPostPayload, Done]
  def assignPostTargetPrincipal: ServiceCall[AssignPostTargetPrincipalPayload, Done]
  def unassignPostTargetPrincipal: ServiceCall[UnassignPostTargetPrincipalPayload, Done]
  def deletePost: ServiceCall[DeletePostPayload, Done]
  def getPostById(
    id: PostId,
    fromReadSide: Boolean = true,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[NotUsed, Post]
  def getPostsById(
    fromReadSide: Boolean = true,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[Set[PostId], Seq[Post]]
  def getPostViews: ServiceCall[GetPostViewsPayload, Seq[PostView]]
  def canAccessToPost: ServiceCall[CanAccessToPostPayload, Boolean]
  def findPosts: ServiceCall[PostFindQuery, FindResult]

  // ************************** CMS Post Metrics **************************

  def viewPost: ServiceCall[ViewPostPayload, Done]
  def likePost: ServiceCall[LikePostPayload, Done]
  def unlikePost: ServiceCall[UnlikePostPayload, Done]
  def getPostMetricById: ServiceCall[GetPostMetricPayload, PostMetric]
  def getPostMetricsById: ServiceCall[GetPostMetricsPayload, Seq[PostMetric]]

  // ************************** CMS Pages **************************

  def createSpaceCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateSpaceCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteSpaceCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getSpaceCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category]
  def getSpaceCategoriesById(fromReadSide: Boolean = true): ServiceCall[Set[CategoryId], Seq[Category]]
  def findSpaceCategories: ServiceCall[CategoryFindQuery, FindResult]

  def createSpace: ServiceCall[CreateSpacePayload, Done]
  def updateSpaceName: ServiceCall[UpdateSpaceNamePayload, Done]
  def updateSpaceDescription: ServiceCall[UpdateSpaceDescriptionPayload, Done]
  def updateSpaceCategoryId: ServiceCall[UpdateSpaceCategoryPayload, Done]
  def assignSpaceTargetPrincipal: ServiceCall[AssignSpaceTargetPrincipalPayload, Done]
  def unassignSpaceTargetPrincipal: ServiceCall[UnassignSpaceTargetPrincipalPayload, Done]
  def activateSpace: ServiceCall[ActivateSpacePayload, Done]
  def deactivateSpace: ServiceCall[DeactivateSpacePayload, Done]
  def deleteSpace: ServiceCall[DeleteSpacePayload, Done]
  def getSpaceById(id: SpaceId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Space]
  def getSpacesById(fromReadSide: Boolean = true): ServiceCall[Set[SpaceId], Seq[Space]]
  def getSpaceViews: ServiceCall[GetSpaceViewsPayload, Seq[SpaceView]]
  def canAccessToSpace: ServiceCall[CanAccessToSpacePayload, Boolean]
  def findSpaces: ServiceCall[SpaceFindQuery, FindResult]

  def createPage: ServiceCall[CreatePagePayload, Done]
  def updatePageFeatured: ServiceCall[UpdatePageFeaturedPayload, Done]
  def updatePageAuthor: ServiceCall[UpdatePageAuthorPayload, Done]
  def updatePageTitle: ServiceCall[UpdatePageTitlePayload, Done]
  def updatePageWidgetContent: ServiceCall[UpdatePageWidgetContentPayload, Done]
  def changePageWidgetContentOrder: ServiceCall[ChangePageWidgetContentOrderPayload, Done]
  def deletePageWidgetContent: ServiceCall[DeletePageWidgetContentPayload, Done]
  def updatePagePublicationTimestamp: ServiceCall[UpdatePagePublicationTimestampPayload, Done]
  def publishPage: ServiceCall[PublishPagePayload, Done]
  def unpublishPage: ServiceCall[UnpublishPagePayload, Done]
  def assignPageTargetPrincipal: ServiceCall[AssignPageTargetPrincipalPayload, Done]
  def unassignPageTargetPrincipal: ServiceCall[UnassignPageTargetPrincipalPayload, Done]
  def deletePage: ServiceCall[DeletePagePayload, Done]
  def getPageById(
    id: PageId,
    fromReadSide: Boolean = true,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[NotUsed, Page]
  def getPagesById(
    fromReadSide: Boolean = true,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[Set[PageId], Seq[Page]]
  def getPageViews: ServiceCall[GetPageViewsPayload, Seq[PageView]]
  def canAccessToPage: ServiceCall[CanAccessToPagePayload, Boolean]
  def findPages: ServiceCall[PageFindQuery, FindResult]

  // ************************** CMS Page Metrics **************************

  def viewPage: ServiceCall[ViewPagePayload, Done]
  def likePage: ServiceCall[LikePagePayload, Done]
  def unlikePage: ServiceCall[UnlikePagePayload, Done]
  def getPageMetricById: ServiceCall[GetPageMetricPayload, PageMetric]
  def getPageMetricsById: ServiceCall[GetPageMetricsPayload, Seq[PageMetric]]

  final override def descriptor = {
    import Service._
    named("cms")
      .withCalls(
        // ************************** CMS Files **************************
        pathCall("/api/cms/v1/storeFile", storeFile),
        pathCall("/api/cms/v1/removeFile", removeFile),
        pathCall("/api/cms/v1/removeFiles", removeFiles),
        pathCall("/api/cms/v1/getFiles/:objectId", getFiles _),
        // ************************** CMS Blogs **************************
        pathCall("/api/cms/v1/createBlogCategory", createBlogCategory),
        pathCall("/api/cms/v1/updateBlogCategory", updateBlogCategory),
        pathCall("/api/cms/v1/deleteBlogCategory", deleteBlogCategory),
        pathCall("/api/cms/v1/getBlogCategoryById/:id/:fromReadSide", getBlogCategoryById _),
        pathCall("/api/cms/v1/getBlogCategoriesById/:fromReadSide", getBlogCategoriesById _),
        pathCall("/api/cms/v1/findBlogCategories", findBlogCategories),
        pathCall("/api/cms/v1/createBlog", createBlog),
        pathCall("/api/cms/v1/updateBlogName", updateBlogName),
        pathCall("/api/cms/v1/updateBlogDescription", updateBlogDescription),
        pathCall("/api/cms/v1/updateBlogCategoryId", updateBlogCategoryId),
        pathCall("/api/cms/v1/assignBlogTargetPrincipal", assignBlogTargetPrincipal),
        pathCall("/api/cms/v1/unassignBlogTargetPrincipal", unassignBlogTargetPrincipal),
        pathCall("/api/cms/v1/activateBlog", activateBlog),
        pathCall("/api/cms/v1/deactivateBlog", deactivateBlog),
        pathCall("/api/cms/v1/deleteBlog", deleteBlog),
        pathCall("/api/cms/v1/getBlogById/:id/:fromReadSide", getBlogById _),
        pathCall("/api/cms/v1/getBlogsById/:fromReadSide", getBlogsById _),
        pathCall("/api/cms/v1/getBlogViews", getBlogViews),
        pathCall("/api/cms/v1/canAccessToBlog", canAccessToBlog),
        pathCall("/api/cms/v1/findBlogs", findBlogs),
        pathCall("/api/cms/v1/createPost", createPost),
        pathCall("/api/cms/v1/updatePostFeatured", updatePostFeatured),
        pathCall("/api/cms/v1/updatePostAuthor", updatePostAuthor),
        pathCall("/api/cms/v1/updatePostTitle", updatePostTitle),
        pathCall("/api/cms/v1/updatePostWidgetContent", updatePostWidgetContent),
        pathCall("/api/cms/v1/changePostWidgetContentOrder", changePostWidgetContentOrder),
        pathCall("/api/cms/v1/deletePostWidgetContent", deletePostWidgetContent),
        pathCall("/api/cms/v1/updatePostPublicationTimestamp", updatePostPublicationTimestamp),
        pathCall("/api/cms/v1/publishPost", publishPost),
        pathCall("/api/cms/v1/unpublishPost", unpublishPost),
        pathCall("/api/cms/v1/assignPostTargetPrincipal", assignPostTargetPrincipal),
        pathCall("/api/cms/v1/unassignPostTargetPrincipal", unassignPostTargetPrincipal),
        pathCall("/api/cms/v1/deletePost", deletePost),
        pathCall("/api/cms/v1/getPostById/:id/:fromReadSide?withIntro&withContent&withTargets", getPostById _),
        pathCall("/api/cms/v1/getPostsById/:fromReadSide?withIntro&withContent&withTargets", getPostsById _),
        pathCall("/api/cms/v1/getPostViews", getPostViews),
        pathCall("/api/cms/v1/canAccessToPost", canAccessToPost),
        pathCall("/api/cms/v1/findPosts", findPosts),
        pathCall("/api/cms/v1/viewPost", viewPost),
        pathCall("/api/cms/v1/likePost", likePost),
        pathCall("/api/cms/v1/unlikePost", unlikePost),
        pathCall("/api/cms/v1/getPostMetricById", getPostMetricById),
        pathCall("/api/cms/v1/getPostMetricsById", getPostMetricsById),
        // ************************** CMS Pages **************************
        pathCall("/api/cms/v1/createSpaceCategory", createSpaceCategory),
        pathCall("/api/cms/v1/updateSpaceCategory", updateSpaceCategory),
        pathCall("/api/cms/v1/deleteSpaceCategory", deleteSpaceCategory),
        pathCall("/api/cms/v1/getSpaceCategoryById/:id/:fromReadSide", getSpaceCategoryById _),
        pathCall("/api/cms/v1/getSpaceCategoriesById/:fromReadSide", getSpaceCategoriesById _),
        pathCall("/api/cms/v1/findSpaceCategories", findSpaceCategories),
        pathCall("/api/cms/v1/createSpace", createSpace),
        pathCall("/api/cms/v1/updateSpaceName", updateSpaceName),
        pathCall("/api/cms/v1/updateSpaceDescription", updateSpaceDescription),
        pathCall("/api/cms/v1/updateSpaceCategoryId", updateSpaceCategoryId),
        pathCall("/api/cms/v1/assignSpaceTargetPrincipal", assignSpaceTargetPrincipal),
        pathCall("/api/cms/v1/unassignSpaceTargetPrincipal", unassignSpaceTargetPrincipal),
        pathCall("/api/cms/v1/activateSpace", activateSpace),
        pathCall("/api/cms/v1/deactivateSpace", deactivateSpace),
        pathCall("/api/cms/v1/deleteSpace", deleteSpace),
        pathCall("/api/cms/v1/getSpaceById/:id/:fromReadSide", getSpaceById _),
        pathCall("/api/cms/v1/getSpacesById/:fromReadSide", getSpacesById _),
        pathCall("/api/cms/v1/getSpaceViews", getSpaceViews),
        pathCall("/api/cms/v1/canAccessToSpace", canAccessToSpace),
        pathCall("/api/cms/v1/findSpaces", findSpaces),
        pathCall("/api/cms/v1/createPage", createPage),
        pathCall("/api/cms/v1/updatePageFeatured", updatePageFeatured),
        pathCall("/api/cms/v1/updatePageAuthor", updatePageAuthor),
        pathCall("/api/cms/v1/updatePageTitle", updatePageTitle),
        pathCall("/api/cms/v1/updatePageWidgetContent", updatePageWidgetContent),
        pathCall("/api/cms/v1/changePageWidgetContentOrder", changePageWidgetContentOrder),
        pathCall("/api/cms/v1/deletePageWidgetContent", deletePageWidgetContent),
        pathCall("/api/cms/v1/updatePagePublicationTimestamp", updatePagePublicationTimestamp),
        pathCall("/api/cms/v1/publishPage", publishPage),
        pathCall("/api/cms/v1/unpublishPage", unpublishPage),
        pathCall("/api/cms/v1/assignPageTargetPrincipal", assignPageTargetPrincipal),
        pathCall("/api/cms/v1/unassignPageTargetPrincipal", unassignPageTargetPrincipal),
        pathCall("/api/cms/v1/deletePage", deletePage),
        pathCall("/api/cms/v1/getPageById/:id/:fromReadSide?withIntro&withContent&withTargets", getPageById _),
        pathCall("/api/cms/v1/getPagesById/:fromReadSide?withIntro&withContent&withTargets", getPagesById _),
        pathCall("/api/cms/v1/getPageViews", getPageViews),
        pathCall("/api/cms/v1/canAccessToPage", canAccessToPage),
        pathCall("/api/cms/v1/findPages", findPages),
        pathCall("/api/cms/v1/viewPage", viewPage),
        pathCall("/api/cms/v1/likePage", likePage),
        pathCall("/api/cms/v1/unlikePage", unlikePage),
        pathCall("/api/cms/v1/getPageMetricById", getPageMetricById),
        pathCall("/api/cms/v1/getPageMetricsById", getPageMetricsById)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
