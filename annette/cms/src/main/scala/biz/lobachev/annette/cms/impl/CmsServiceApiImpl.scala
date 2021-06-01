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
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.api.category._
import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.cms.impl.space._
import biz.lobachev.annette.cms.impl.category._
import biz.lobachev.annette.cms.impl.post._
import biz.lobachev.annette.core.model.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

class CmsServiceApiImpl(
  categoryEntityService: CategoryEntityService,
  spaceEntityService: SpaceEntityService,
  postEntityService: PostEntityService
) extends CmsServiceApi {

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

  override def createSpace: ServiceCall[CreateSpacePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.createSpace(payload)
    }

  override def updateSpaceName: ServiceCall[UpdateSpaceNamePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.updateSpaceName(payload)
    }

  override def updateSpaceDescription: ServiceCall[UpdateSpaceDescriptionPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.updateSpaceDescription(payload)
    }

  override def updateSpaceCategory: ServiceCall[UpdateSpaceCategoryPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.updateSpaceCategory(payload)
    }

  override def assignSpaceTargetPrincipal: ServiceCall[AssignSpaceTargetPrincipalPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.assignSpaceTargetPrincipal(payload)
    }

  override def unassignSpaceTargetPrincipal: ServiceCall[UnassignSpaceTargetPrincipalPayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.unassignSpaceTargetPrincipal(payload)
    }

  override def activateSpace: ServiceCall[ActivateSpacePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.activateSpace(payload)
    }

  override def deactivateSpace: ServiceCall[DeactivateSpacePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.deactivateSpace(payload)
    }

  override def deleteSpace: ServiceCall[DeleteSpacePayload, Done] =
    ServiceCall { payload =>
      spaceEntityService.deleteSpace(payload)
    }

  override def getSpaceById(id: SpaceId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Space] =
    ServiceCall { _ =>
      spaceEntityService.getSpaceById(id, fromReadSide)
    }

  override def getSpaceAnnotationById(
    id: SpaceId,
    fromReadSide: Boolean = true
  ): ServiceCall[NotUsed, SpaceAnnotation] =
    ServiceCall { _ =>
      spaceEntityService.getSpaceAnnotationById(id, fromReadSide)
    }

  override def getSpacesById(fromReadSide: Boolean = true): ServiceCall[Set[SpaceId], Map[SpaceId, Space]] =
    ServiceCall { ids =>
      spaceEntityService.getSpacesById(ids, fromReadSide)
    }

  override def getSpaceAnnotationsById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[SpaceId], Map[SpaceId, SpaceAnnotation]] =
    ServiceCall { ids =>
      spaceEntityService.getSpaceAnnotationsById(ids, fromReadSide)
    }

  override def findSpaces: ServiceCall[SpaceFindQuery, FindResult] =
    ServiceCall { query =>
      spaceEntityService.findSpaces(query)
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

  override def getPostMetricById(id: PostId): ServiceCall[NotUsed, PostMetric] =
    ServiceCall { _ =>
      postEntityService.getPostMetricById(id)
    }

  override def getPostMetricsById: ServiceCall[Set[PostId], Map[PostId, PostMetric]] =
    ServiceCall { ids =>
      postEntityService.getPostMetricsById(ids)
    }

  override def movePost: ServiceCall[MovePostPayload, Done] = ???
}
