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

package biz.lobachev.annette.cms.impl.blogs.post.dao

import biz.lobachev.annette.cms.api.blogs.post.{ContentTypes, PostFindQuery}
import biz.lobachev.annette.cms.impl.blogs.post.PostEntity
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class PostIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.post-index"

  def createPost(event: PostEntity.PostCreated) =
    createIndexDoc(
      event.id,
      "id"                -> event.id,
      "blogId"            -> event.blogId,
      "featured"          -> event.featured,
      "authorId"          -> event.authorId.code,
      "title"             -> event.title,
      "intro"             -> event.introContent.content.values.map(_.indexData).flatten.mkString("\n"),
      "content"           -> event.content.content.values.map(_.indexData).flatten.mkString("\n"),
      "publicationStatus" -> "draft",
      "targets"           -> event.targets.map(_.code),
      "updatedBy"         -> event.createdBy.code,
      "updatedAt"         -> event.createdAt
    )

  def updatePostFeatured(event: PostEntity.PostFeaturedUpdated) =
    updateIndexDoc(
      event.id,
      "featured"  -> event.featured,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def updatePostAuthor(event: PostEntity.PostAuthorUpdated) =
    updateIndexDoc(
      event.id,
      "authorId"  -> event.authorId.code,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def updatePostTitle(event: PostEntity.PostTitleUpdated) =
    updateIndexDoc(
      event.id,
      "title"     -> event.title,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def updatePostWidgetContent(event: PostEntity.PostWidgetContentUpdated) =
    updateIndexDoc(
      event.id,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def changeWidgetContentOrder(event: PostEntity.WidgetContentOrderChanged) =
    updateIndexDoc(
      event.id,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def deleteWidgetContent(event: PostEntity.WidgetContentDeleted) =
    updateIndexDoc(
      event.id,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def changePostIndex(event: PostEntity.PostIndexChanged) = {
    val alias = event.contentType match {
      case ContentTypes.Intro => "intro"
      case ContentTypes.Post  => "content"
    }
    updateIndexDoc(
      event.id,
      alias -> event.indexData,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )
  }

  def updatePostPublicationTimestamp(event: PostEntity.PostPublicationTimestampUpdated) =
    updateIndexDoc(
      event.id,
      "publicationTimestamp" -> event.publicationTimestamp.orNull,
      "updatedAt"            -> event.updatedAt,
      "updatedBy"            -> event.updatedBy.code
    )

  def publishPost(event: PostEntity.PostPublished) =
    updateIndexDoc(
      event.id,
      "publicationStatus"    -> "published",
      "publicationTimestamp" -> event.publicationTimestamp,
      "updatedAt"            -> event.updatedAt,
      "updatedBy"            -> event.updatedBy.code
    )

  def unpublishPost(event: PostEntity.PostUnpublished) =
    updateIndexDoc(
      event.id,
      "publicationStatus" -> "draft",
      "updatedAt"         -> event.updatedAt,
      "updatedBy"         -> event.updatedBy.code
    )

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned) = {
    val targetsField = alias2FieldName("targets")
    for {
      _ <- client.execute {
             updateById(indexName, event.id)
               .script(s"""ctx._source.${targetsField}.add("${event.principal.code}")""")
               .refresh(RefreshPolicy.Immediate)
           }.map(processResponse)

      _ <- client.execute {
             updateById(indexName, event.id)
               .doc(
                 alias2FieldName("updatedAt") -> event.updatedAt,
                 alias2FieldName("updatedBy") -> event.updatedBy.code
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse)
    } yield ()
  }

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned) = {
    val targetsField = alias2FieldName("targets")
    for {
      _ <- client.execute {
             updateById(indexName, event.id)
               .script(
                 s"""if (ctx._source.${targetsField}.contains("${event.principal.code}")) { ctx._source.${targetsField}.remove(ctx._source.${targetsField}.indexOf("${event.principal.code}")) }"""
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse)
      _ <- client.execute {
             updateById(indexName, event.id)
               .doc(
                 alias2FieldName("updatedAt") -> event.updatedAt,
                 alias2FieldName("updatedBy") -> event.updatedBy.code
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse)
    } yield ()
  }

  def deletePost(event: PostEntity.PostDeleted) =
    deleteIndexDoc(event.id)

  def findPosts(query: PostFindQuery): Future[FindResult] = {

    val filterQuery                   = buildFilterQuery(
      query.filter,
      Seq("title" -> 3.0, "intro" -> 2.0, "content" -> 1.0)
    )
    val postIdsQuery                  = query.postIds.map(postIds => termsSetQuery(alias2FieldName("id"), postIds, script("1"))).toSeq
    val blogQuery                     = query.blogs.map(blogs => termsSetQuery(alias2FieldName("blogId"), blogs, script("1"))).toSeq
    val featuredQuery                 = query.featured.map(matchQuery(alias2FieldName("featured"), _)).toSeq
    val authorsQuery                  =
      query.authors.map(authors => termsSetQuery(alias2FieldName("authorId"), authors.map(_.code), script("1"))).toSeq
    val publicationStatusQuery        = query.publicationStatus.map(matchQuery(alias2FieldName("publicationStatus"), _)).toSeq
    val publicationTimestampFromQuery =
      query.publicationTimestampFrom.map(from =>
        rangeQuery(alias2FieldName("publicationTimestamp")).gte(ElasticDate(from.toString))
      )
    val publicationTimestampToQuery   =
      query.publicationTimestampTo.map(to =>
        rangeQuery(alias2FieldName("publicationTimestamp")).lte(ElasticDate(to.toString))
      )
    val targetsQuery                  =
      query.targets.map(targets => termsSetQuery(alias2FieldName("targets"), targets.map(_.code), script("1"))).toSeq
    val sortBy: Seq[FieldSort]        = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(
        must(
          filterQuery ++ postIdsQuery ++ blogQuery ++ featuredQuery ++
            authorsQuery ++ publicationStatusQuery ++ publicationTimestampFromQuery ++
            publicationTimestampToQuery ++ targetsQuery
        )
      )
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

}
