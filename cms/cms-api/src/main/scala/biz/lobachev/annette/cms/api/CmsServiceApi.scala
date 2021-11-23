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
  AssignTargetPrincipalPayload,
  CanAccessToEntityPayload,
  DeactivatePayload,
  DeletePayload,
  UnassignTargetPrincipalPayload,
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
  def updateBlogName: ServiceCall[UpdateNamePayload, Done]
  def updateBlogDescription: ServiceCall[UpdateDescriptionPayload, Done]
  def updateBlogCategoryId: ServiceCall[common.UpdateCategoryIdPayload, Done]
  def assignBlogTargetPrincipal: ServiceCall[AssignTargetPrincipalPayload, Done]
  def unassignBlogTargetPrincipal: ServiceCall[UnassignTargetPrincipalPayload, Done]
  def activateBlog: ServiceCall[ActivatePayload, Done]
  def deactivateBlog: ServiceCall[DeactivatePayload, Done]
  def deleteBlog: ServiceCall[DeletePayload, Done]
  def getBlogById(id: BlogId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Blog]
  def getBlogsById(fromReadSide: Boolean = true): ServiceCall[Set[BlogId], Seq[Blog]]
  def getBlogViews: ServiceCall[GetBlogViewsPayload, Seq[BlogView]]
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
  def assignPostTargetPrincipal: ServiceCall[AssignTargetPrincipalPayload, Updated]
  def unassignPostTargetPrincipal: ServiceCall[UnassignTargetPrincipalPayload, Updated]
  def deletePost: ServiceCall[DeletePayload, Updated]
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
  def getPostViews: ServiceCall[GetPostViewsPayload, Seq[Post]]
  def canAccessToPost: ServiceCall[CanAccessToEntityPayload, Boolean]
  def findPosts: ServiceCall[PostFindQuery, FindResult]

  // ************************** CMS Post Metrics **************************

  def viewPost: ServiceCall[article.ViewPayload, Done]
  def likePost: ServiceCall[LikePayload, Done]
  def unlikePost: ServiceCall[UnlikePayload, Done]
  def getPostMetricById: ServiceCall[GetMetricPayload, Metric]
  def getPostMetricsById: ServiceCall[GetMetricsPayload, Seq[Metric]]

  // ************************** CMS Pages **************************

  def createSpaceCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateSpaceCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteSpaceCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getSpaceCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category]
  def getSpaceCategoriesById(fromReadSide: Boolean = true): ServiceCall[Set[CategoryId], Seq[Category]]
  def findSpaceCategories: ServiceCall[CategoryFindQuery, FindResult]

  def createSpace: ServiceCall[CreateSpacePayload, Done]
  def updateSpaceName: ServiceCall[UpdateNamePayload, Done]
  def updateSpaceDescription: ServiceCall[UpdateDescriptionPayload, Done]
  def updateSpaceCategoryId: ServiceCall[UpdateCategoryIdPayload, Done]
  def assignSpaceTargetPrincipal: ServiceCall[AssignTargetPrincipalPayload, Done]
  def unassignSpaceTargetPrincipal: ServiceCall[UnassignTargetPrincipalPayload, Done]
  def activateSpace: ServiceCall[ActivatePayload, Done]
  def deactivateSpace: ServiceCall[DeactivatePayload, Done]
  def deleteSpace: ServiceCall[DeletePayload, Done]
  def getSpaceById(id: SpaceId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Space]
  def getSpacesById(fromReadSide: Boolean = true): ServiceCall[Set[SpaceId], Seq[Space]]
  def getSpaceViews: ServiceCall[GetSpaceViewsPayload, Seq[SpaceView]]
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
  def assignPageTargetPrincipal: ServiceCall[AssignTargetPrincipalPayload, Updated]
  def unassignPageTargetPrincipal: ServiceCall[UnassignTargetPrincipalPayload, Updated]
  def deletePage: ServiceCall[DeletePayload, Updated]
  def getPageById(
    id: PageId,
    fromReadSide: Boolean = true,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[NotUsed, Page]
  def getPagesById(
    fromReadSide: Boolean = true,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ): ServiceCall[Set[PageId], Seq[Page]]
  def getPageViews: ServiceCall[GetPageViewsPayload, Seq[Page]]
  def canAccessToPage: ServiceCall[CanAccessToEntityPayload, Boolean]
  def findPages: ServiceCall[PageFindQuery, FindResult]

  // ************************** CMS Page Metrics **************************

  def viewPage: ServiceCall[article.ViewPayload, Done]
  def likePage: ServiceCall[LikePayload, Done]
  def unlikePage: ServiceCall[UnlikePayload, Done]
  def getPageMetricById: ServiceCall[GetMetricPayload, Metric]
  def getPageMetricsById: ServiceCall[GetMetricsPayload, Seq[Metric]]

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
        pathCall("/api/cms/v1/getPageById/:id/:fromReadSide?withContent&withTargets", getPageById _),
        pathCall("/api/cms/v1/getPagesById/:fromReadSide?withContent&withTargets", getPagesById _),
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
