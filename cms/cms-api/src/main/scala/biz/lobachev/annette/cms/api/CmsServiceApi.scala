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
import biz.lobachev.annette.cms.api.common.article.{
  GetMetricPayload,
  GetMetricsPayload,
  LikePayload,
  Metric,
  PublishPayload,
  UnlikePayload,
  UnpublishPayload,
  UpdateAuthorPayload,
  UpdatePublicationTimestampPayload,
  UpdateTitlePayload
}
import biz.lobachev.annette.cms.api.common.{
  article,
  ActivatePayload,
  AssignPrincipalPayload,
  CanAccessToEntityPayload,
  DeactivatePayload,
  DeletePayload,
  UnassignPrincipalPayload,
  UpdateCategoryIdPayload,
  UpdateDescriptionPayload,
  UpdateNamePayload,
  Updated
}
import biz.lobachev.annette.cms.api.content.{
  ChangeWidgetOrderPayload,
  DeleteWidgetPayload,
  UpdateContentSettingsPayload,
  UpdateWidgetPayload
}
import biz.lobachev.annette.cms.api.files._
import biz.lobachev.annette.cms.api.home_pages.{
  AssignHomePagePayload,
  HomePage,
  HomePageFindQuery,
  HomePageId,
  UnassignHomePagePayload
}
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
  def getBlogCategory(id: CategoryId, source: Option[String] = None): ServiceCall[NotUsed, Category]
  def getBlogCategories(source: Option[String] = None): ServiceCall[Set[CategoryId], Seq[Category]]
  def findBlogCategories: ServiceCall[CategoryFindQuery, FindResult]

  def createBlog: ServiceCall[CreateBlogPayload, Done]
  def updateBlogName: ServiceCall[UpdateNamePayload, Done]
  def updateBlogDescription: ServiceCall[UpdateDescriptionPayload, Done]
  def updateBlogCategoryId: ServiceCall[common.UpdateCategoryIdPayload, Done]
  def assignBlogAuthorPrincipal: ServiceCall[AssignPrincipalPayload, Done]
  def unassignBlogAuthorPrincipal: ServiceCall[UnassignPrincipalPayload, Done]
  def assignBlogTargetPrincipal: ServiceCall[AssignPrincipalPayload, Done]
  def unassignBlogTargetPrincipal: ServiceCall[UnassignPrincipalPayload, Done]
  def activateBlog: ServiceCall[ActivatePayload, Done]
  def deactivateBlog: ServiceCall[DeactivatePayload, Done]
  def deleteBlog: ServiceCall[DeletePayload, Done]
  def getBlog(id: BlogId, source: Option[String] = None): ServiceCall[NotUsed, Blog]
  def getBlogs(source: Option[String] = None): ServiceCall[Set[BlogId], Seq[Blog]]
  def getBlogViews: ServiceCall[GetBlogViewsPayload, Seq[BlogView]]
  def canEditBlogPosts: ServiceCall[CanAccessToEntityPayload, Boolean]
  def canAccessToBlog: ServiceCall[CanAccessToEntityPayload, Boolean]
  def findBlogs: ServiceCall[BlogFindQuery, FindResult]

  def createPost: ServiceCall[CreatePostPayload, Post]
  def updatePostFeatured: ServiceCall[UpdatePostFeaturedPayload, Updated]
  def updatePostAuthor: ServiceCall[UpdateAuthorPayload, Updated]
  def updatePostTitle: ServiceCall[UpdateTitlePayload, Updated]
  def updatePostContentSettings: ServiceCall[UpdateContentSettingsPayload, Updated]
  def updatePostWidget: ServiceCall[UpdateWidgetPayload, Updated]
  def changePostWidgetOrder: ServiceCall[ChangeWidgetOrderPayload, Updated]
  def deletePostWidget: ServiceCall[DeleteWidgetPayload, Updated]
  def updatePostPublicationTimestamp: ServiceCall[UpdatePublicationTimestampPayload, Updated]
  def publishPost: ServiceCall[PublishPayload, Updated]
  def unpublishPost: ServiceCall[UnpublishPayload, Updated]
  def assignPostTargetPrincipal: ServiceCall[AssignPrincipalPayload, Updated]
  def unassignPostTargetPrincipal: ServiceCall[UnassignPrincipalPayload, Updated]
  def deletePost: ServiceCall[DeletePayload, Updated]
  def getPost(
    id: PostId,
    source: Option[String] = None,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[NotUsed, Post]
  def getPosts(
    source: Option[String] = None,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[Set[PostId], Seq[Post]]
  def getPostViews: ServiceCall[GetPostViewsPayload, Seq[Post]]
  def canEditPost: ServiceCall[CanAccessToEntityPayload, Boolean]
  def canAccessToPost: ServiceCall[CanAccessToEntityPayload, Boolean]
  def findPosts: ServiceCall[PostFindQuery, FindResult]

  // ************************** CMS Post Metrics **************************

  def viewPost: ServiceCall[article.ViewPayload, Done]
  def likePost: ServiceCall[LikePayload, Done]
  def unlikePost: ServiceCall[UnlikePayload, Done]
  def getPostMetric: ServiceCall[GetMetricPayload, Metric]
  def getPostMetrics: ServiceCall[GetMetricsPayload, Seq[Metric]]

  // ************************** CMS Pages **************************

  def createSpaceCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateSpaceCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteSpaceCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getSpaceCategory(id: CategoryId, source: Option[String] = None): ServiceCall[NotUsed, Category]
  def getSpaceCategories(source: Option[String] = None): ServiceCall[Set[CategoryId], Seq[Category]]
  def findSpaceCategories: ServiceCall[CategoryFindQuery, FindResult]

  def createSpace: ServiceCall[CreateSpacePayload, Done]
  def updateSpaceName: ServiceCall[UpdateNamePayload, Done]
  def updateSpaceDescription: ServiceCall[UpdateDescriptionPayload, Done]
  def updateSpaceCategoryId: ServiceCall[UpdateCategoryIdPayload, Done]
  def assignSpaceAuthorPrincipal: ServiceCall[AssignPrincipalPayload, Done]
  def unassignSpaceAuthorPrincipal: ServiceCall[UnassignPrincipalPayload, Done]
  def assignSpaceTargetPrincipal: ServiceCall[AssignPrincipalPayload, Done]
  def unassignSpaceTargetPrincipal: ServiceCall[UnassignPrincipalPayload, Done]
  def activateSpace: ServiceCall[ActivatePayload, Done]
  def deactivateSpace: ServiceCall[DeactivatePayload, Done]
  def deleteSpace: ServiceCall[DeletePayload, Done]
  def getSpace(id: SpaceId, source: Option[String] = None): ServiceCall[NotUsed, Space]
  def getSpaces(source: Option[String] = None): ServiceCall[Set[SpaceId], Seq[Space]]
  def getSpaceViews: ServiceCall[GetSpaceViewsPayload, Seq[SpaceView]]
  def canEditSpacePages: ServiceCall[CanAccessToEntityPayload, Boolean]
  def canAccessToSpace: ServiceCall[CanAccessToEntityPayload, Boolean]
  def findSpaces: ServiceCall[SpaceFindQuery, FindResult]

  def createPage: ServiceCall[CreatePagePayload, Page]
  def updatePageAuthor: ServiceCall[UpdateAuthorPayload, Updated]
  def updatePageTitle: ServiceCall[UpdateTitlePayload, Updated]
  def updatePageContentSettings: ServiceCall[UpdateContentSettingsPayload, Updated]
  def updatePageWidget: ServiceCall[UpdateWidgetPayload, Updated]
  def changePageWidgetOrder: ServiceCall[ChangeWidgetOrderPayload, Updated]
  def deletePageWidget: ServiceCall[DeleteWidgetPayload, Updated]
  def updatePagePublicationTimestamp: ServiceCall[UpdatePublicationTimestampPayload, Updated]
  def publishPage: ServiceCall[PublishPayload, Updated]
  def unpublishPage: ServiceCall[UnpublishPayload, Updated]
  def assignPageTargetPrincipal: ServiceCall[AssignPrincipalPayload, Updated]
  def unassignPageTargetPrincipal: ServiceCall[UnassignPrincipalPayload, Updated]
  def deletePage: ServiceCall[DeletePayload, Updated]
  def getPage(
    id: PageId,
    source: Option[String] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[NotUsed, Page]
  def getPages(
    source: Option[String] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[Set[PageId], Seq[Page]]
  def getPageViews: ServiceCall[GetPageViewsPayload, Seq[Page]]
  def canEditPage: ServiceCall[CanAccessToEntityPayload, Boolean]
  def canAccessToPage: ServiceCall[CanAccessToEntityPayload, Boolean]
  def findPages: ServiceCall[PageFindQuery, FindResult]

  // ************************** CMS Page Metrics **************************

  def viewPage: ServiceCall[article.ViewPayload, Done]
  def likePage: ServiceCall[LikePayload, Done]
  def unlikePage: ServiceCall[UnlikePayload, Done]
  def getPageMetric: ServiceCall[GetMetricPayload, Metric]
  def getPageMetrics: ServiceCall[GetMetricsPayload, Seq[Metric]]

  // ************************** CMS Home Page  **************************

  def assignHomePage: ServiceCall[AssignHomePagePayload, Done]
  def unassignHomePage: ServiceCall[UnassignHomePagePayload, Done]
  def getHomePage(
    id: HomePageId,
    source: Option[String] = None
  ): ServiceCall[NotUsed, HomePage]
  def getHomePages(
    source: Option[String] = None
  ): ServiceCall[Set[HomePageId], Seq[HomePage]]
  def getHomePageByPrincipalCodes(applicationId: String): ServiceCall[Seq[String], PageId]
  def findHomePages: ServiceCall[HomePageFindQuery, FindResult]

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
        pathCall("/api/cms/v1/getBlogCategory/:id?source", getBlogCategory _),
        pathCall("/api/cms/v1/getBlogCategories?source", getBlogCategories _),
        pathCall("/api/cms/v1/findBlogCategories", findBlogCategories),
        pathCall("/api/cms/v1/createBlog", createBlog),
        pathCall("/api/cms/v1/updateBlogName", updateBlogName),
        pathCall("/api/cms/v1/updateBlogDescription", updateBlogDescription),
        pathCall("/api/cms/v1/updateBlogCategoryId", updateBlogCategoryId),
        pathCall("/api/cms/v1/assignBlogAuthorPrincipal", assignBlogAuthorPrincipal),
        pathCall("/api/cms/v1/unassignBlogAuthorPrincipal", unassignBlogAuthorPrincipal),
        pathCall("/api/cms/v1/assignBlogTargetPrincipal", assignBlogTargetPrincipal),
        pathCall("/api/cms/v1/unassignBlogTargetPrincipal", unassignBlogTargetPrincipal),
        pathCall("/api/cms/v1/activateBlog", activateBlog),
        pathCall("/api/cms/v1/deactivateBlog", deactivateBlog),
        pathCall("/api/cms/v1/deleteBlog", deleteBlog),
        pathCall("/api/cms/v1/getBlog/:id?source", getBlog _),
        pathCall("/api/cms/v1/getBlogs?source", getBlogs _),
        pathCall("/api/cms/v1/getBlogViews", getBlogViews),
        pathCall("/api/cms/v1/canEditBlogPosts", canEditBlogPosts),
        pathCall("/api/cms/v1/canAccessToBlog", canAccessToBlog),
        pathCall("/api/cms/v1/findBlogs", findBlogs),
        pathCall("/api/cms/v1/createPost", createPost),
        pathCall("/api/cms/v1/updatePostFeatured", updatePostFeatured),
        pathCall("/api/cms/v1/updatePostAuthor", updatePostAuthor),
        pathCall("/api/cms/v1/updatePostTitle", updatePostTitle),
        pathCall("/api/cms/v1/updatePostContentSettings", updatePostContentSettings),
        pathCall("/api/cms/v1/updatePostWidget", updatePostWidget),
        pathCall("/api/cms/v1/changePostWidgetOrder", changePostWidgetOrder),
        pathCall("/api/cms/v1/deletePostWidget", deletePostWidget),
        pathCall("/api/cms/v1/updatePostPublicationTimestamp", updatePostPublicationTimestamp),
        pathCall("/api/cms/v1/publishPost", publishPost),
        pathCall("/api/cms/v1/unpublishPost", unpublishPost),
        pathCall("/api/cms/v1/assignPostTargetPrincipal", assignPostTargetPrincipal),
        pathCall("/api/cms/v1/unassignPostTargetPrincipal", unassignPostTargetPrincipal),
        pathCall("/api/cms/v1/deletePost", deletePost),
        pathCall("/api/cms/v1/getPost/:id?source&withIntro&withContent&withTargets", getPost _),
        pathCall("/api/cms/v1/getPosts?source&withIntro&withContent&withTargets", getPosts _),
        pathCall("/api/cms/v1/getPostViews", getPostViews),
        pathCall("/api/cms/v1/canEditPost", canEditPost),
        pathCall("/api/cms/v1/canAccessToPost", canAccessToPost),
        pathCall("/api/cms/v1/findPosts", findPosts),
        pathCall("/api/cms/v1/viewPost", viewPost),
        pathCall("/api/cms/v1/likePost", likePost),
        pathCall("/api/cms/v1/unlikePost", unlikePost),
        pathCall("/api/cms/v1/getPostMetric", getPostMetric),
        pathCall("/api/cms/v1/getPostMetrics", getPostMetrics),
        // ************************** CMS Pages **************************
        pathCall("/api/cms/v1/createSpaceCategory", createSpaceCategory),
        pathCall("/api/cms/v1/updateSpaceCategory", updateSpaceCategory),
        pathCall("/api/cms/v1/deleteSpaceCategory", deleteSpaceCategory),
        pathCall("/api/cms/v1/getSpaceCategory/:id?source", getSpaceCategory _),
        pathCall("/api/cms/v1/getSpaceCategories?source", getSpaceCategories _),
        pathCall("/api/cms/v1/findSpaceCategories", findSpaceCategories),
        pathCall("/api/cms/v1/createSpace", createSpace),
        pathCall("/api/cms/v1/updateSpaceName", updateSpaceName),
        pathCall("/api/cms/v1/updateSpaceDescription", updateSpaceDescription),
        pathCall("/api/cms/v1/updateSpaceCategoryId", updateSpaceCategoryId),
        pathCall("/api/cms/v1/assignSpaceAuthorPrincipal", assignSpaceAuthorPrincipal),
        pathCall("/api/cms/v1/unassignSpaceAuthorPrincipal", unassignSpaceAuthorPrincipal),
        pathCall("/api/cms/v1/assignSpaceTargetPrincipal", assignSpaceTargetPrincipal),
        pathCall("/api/cms/v1/unassignSpaceTargetPrincipal", unassignSpaceTargetPrincipal),
        pathCall("/api/cms/v1/activateSpace", activateSpace),
        pathCall("/api/cms/v1/deactivateSpace", deactivateSpace),
        pathCall("/api/cms/v1/deleteSpace", deleteSpace),
        pathCall("/api/cms/v1/getSpace/:id?source", getSpace _),
        pathCall("/api/cms/v1/getSpaces?source", getSpaces _),
        pathCall("/api/cms/v1/getSpaceViews", getSpaceViews),
        pathCall("/api/cms/v1/canEditSpacePages", canEditSpacePages),
        pathCall("/api/cms/v1/canAccessToSpace", canAccessToSpace),
        pathCall("/api/cms/v1/findSpaces", findSpaces),
        pathCall("/api/cms/v1/createPage", createPage),
        pathCall("/api/cms/v1/updatePageAuthor", updatePageAuthor),
        pathCall("/api/cms/v1/updatePageTitle", updatePageTitle),
        pathCall("/api/cms/v1/updatePageContentSettings", updatePageContentSettings),
        pathCall("/api/cms/v1/updatePageWidget", updatePageWidget),
        pathCall("/api/cms/v1/changePageWidgetOrder", changePageWidgetOrder),
        pathCall("/api/cms/v1/deletePageWidget", deletePageWidget),
        pathCall("/api/cms/v1/updatePagePublicationTimestamp", updatePagePublicationTimestamp),
        pathCall("/api/cms/v1/publishPage", publishPage),
        pathCall("/api/cms/v1/unpublishPage", unpublishPage),
        pathCall("/api/cms/v1/assignPageTargetPrincipal", assignPageTargetPrincipal),
        pathCall("/api/cms/v1/unassignPageTargetPrincipal", unassignPageTargetPrincipal),
        pathCall("/api/cms/v1/deletePage", deletePage),
        pathCall("/api/cms/v1/getPage/:id?source&withContent&withTargets", getPage _),
        pathCall("/api/cms/v1/getPages?source&withContent&withTargets", getPages _),
        pathCall("/api/cms/v1/getPageViews", getPageViews),
        pathCall("/api/cms/v1/canEditPage", canEditPage),
        pathCall("/api/cms/v1/canAccessToPage", canAccessToPage),
        pathCall("/api/cms/v1/findPages", findPages),
        pathCall("/api/cms/v1/viewPage", viewPage),
        pathCall("/api/cms/v1/likePage", likePage),
        pathCall("/api/cms/v1/unlikePage", unlikePage),
        pathCall("/api/cms/v1/getPageMetric", getPageMetric),
        pathCall("/api/cms/v1/getPageMetrics", getPageMetrics),
        // ************************** CMS Home Page  **************************
        pathCall("/api/cms/v1/assignHomePage", assignHomePage),
        pathCall("/api/cms/v1/unassignHomePage", unassignHomePage),
        pathCall("/api/cms/v1/getHomePage/:id?source", getHomePage _),
        pathCall("/api/cms/v1/getHomePages?source", getHomePages _),
        pathCall("/api/cms/v1/getHomePageByPrincipalCodes/:applicationId", getHomePageByPrincipalCodes _),
        pathCall("/api/cms/v1/findHomePages", findHomePages)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
