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

package biz.lobachev.annette.cms.impl.post.dao

import biz.lobachev.annette.cms.api.post.{HtmlContent, MarkdownContent, PostContent, PostFindQuery}
import biz.lobachev.annette.cms.impl.post.PostEntity
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class PostElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "cms-post"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          keywordField("spaceId"),
          booleanField("featured"),
          keywordField("authorId"),
          textField("title"),
          textField("intro"),
          textField("content"),
          keywordField("publicationStatus"),
          dateField("publicationTimestamp"),
          keywordField("targets"),
          keywordField("updatedBy"),
          dateField("updatedAt")
        )
      )

  def createPost(event: PostEntity.PostCreated): Future[Unit] = {
    val intro   = extractText(event.introContent)
    val content = extractText(event.content)
    elasticClient.execute {
      indexInto(indexName)
        .id(event.id)
        .fields(
          "id"                -> event.id,
          "spaceId"           -> event.spaceId,
          "featured"          -> event.featured,
          "authorId"          -> event.authorId.code,
          "title"             -> event.title,
          "intro"             -> intro,
          "content"           -> content,
          "publicationStatus" -> "draft",
          "targets"           -> event.targets.map(_.code),
          "updatedBy"         -> event.createdBy.code,
          "updatedAt"         -> event.createdAt
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("createPost", event.id)(_))
  }

  private def extractText(content: PostContent): String =
    content match {
      case MarkdownContent(markdown) => markdown
      case HtmlContent(html)         => html
    }

  def updatePostFeatured(event: PostEntity.PostFeaturedUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "featured"  -> event.featured,
          "updatedAt" -> event.updatedAt,
          "updatedBy" -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updatePostFeatured", event.id)(_))

  def updatePostAuthor(event: PostEntity.PostAuthorUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "authorId"  -> event.authorId.code,
          "updatedAt" -> event.updatedAt,
          "updatedBy" -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updatePostAuthor", event.id)(_))

  def updatePostTitle(event: PostEntity.PostTitleUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "title"     -> event.title,
          "updatedAt" -> event.updatedAt,
          "updatedBy" -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)

    }
      .map(processResponse("updatePostTitle", event.id)(_))

  def updatePostIntro(event: PostEntity.PostIntroUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "intro"     -> extractText(event.introContent),
          "updatedAt" -> event.updatedAt,
          "updatedBy" -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updatePostIntro", event.id)(_))

  def updatePostContent(event: PostEntity.PostContentUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "content"   -> extractText(event.content),
          "updatedAt" -> event.updatedAt,
          "updatedBy" -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updatePostContent", event.id)(_))

  def updatePostPublicationTimestamp(event: PostEntity.PostPublicationTimestampUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "publicationTimestamp" -> event.publicationTimestamp.orNull,
          "updatedAt"            -> event.updatedAt,
          "updatedBy"            -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updatePostPublicationTimestamp", event.id)(_))

  def publishPost(event: PostEntity.PostPublished): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "publicationStatus"    -> "published",
          "publicationTimestamp" -> event.publicationTimestamp,
          "updatedAt"            -> event.updatedAt,
          "updatedBy"            -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("publishPost", event.id)(_))

  def unpublishPost(event: PostEntity.PostUnpublished): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "publicationStatus" -> "draft",
          "updatedAt"         -> event.updatedAt,
          "updatedBy"         -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("unpublishPost", event.id)(_))

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned): Future[Unit] =
    for {
      _ <- elasticClient.execute {
             updateById(indexName, event.id)
               .script(s"""ctx._source.targets.add("${event.principal.code}")""")
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse("assignPostTargetPrincipal", event.id)(_))
      _ <- elasticClient.execute {
             updateById(indexName, event.id)
               .doc(
                 "updatedAt" -> event.updatedAt,
                 "updatedBy" -> event.updatedBy.code
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse("unassignPostTargetPrincipal2", event.id)(_))
    } yield ()

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned): Future[Unit] =
    for {
      _ <- elasticClient.execute {
             updateById(indexName, event.id)
               .script(
                 s"""if (ctx._source.targets.contains("${event.principal.code}")) { ctx._source.targets.remove(ctx._source.targets.indexOf("${event.principal.code}")) }"""
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse("unassignPostTargetPrincipal1", event.id)(_))
      _ <- elasticClient.execute {
             updateById(indexName, event.id)
               .doc(
                 "updatedAt" -> event.updatedAt,
                 "updatedBy" -> event.updatedBy.code
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse("unassignPostTargetPrincipal2", event.id)(_))
    } yield ()

  def deletePost(event: PostEntity.PostDeleted): Future[Unit] =
    elasticClient.execute {
      deleteById(indexName, event.id)
    }
      .map(processResponse("deletePost", event.id)(_))

  def findPosts(query: PostFindQuery): Future[FindResult] = ???

  //  private def processResponse[T](method: String, id: String): PartialFunction[Response[T], Unit] = {
//    case success: RequestSuccess[_] =>
//      log.debug("{}( {} ): {}", method, id, success)
//    case failure: RequestFailure    =>
//      log.error("{}( {} ): {}", method, id, failure)
//      throw failure.error.asException
//  }
}
