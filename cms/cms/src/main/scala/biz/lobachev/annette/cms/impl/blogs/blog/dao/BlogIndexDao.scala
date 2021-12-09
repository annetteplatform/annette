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

package biz.lobachev.annette.cms.impl.blogs.blog.dao

import biz.lobachev.annette.cms.api.blogs.blog.BlogFindQuery
import biz.lobachev.annette.cms.impl.blogs.blog.BlogEntity
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class BlogIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.blog-index"

  def createBlog(event: BlogEntity.BlogCreated) =
    createIndexDoc(
      event.id,
      "id"          -> event.id,
      "name"        -> event.name,
      "description" -> event.description,
      "categoryId"  -> event.categoryId,
      "authors"     -> event.authors.map(_.code),
      "targets"     -> event.targets.map(_.code),
      "active"      -> true,
      "updatedBy"   -> event.createdBy.code,
      "updatedAt"   -> event.createdAt
    )

  def updateBlogName(event: BlogEntity.BlogNameUpdated) =
    updateIndexDoc(
      event.id,
      "name"      -> event.name,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def updateBlogDescription(event: BlogEntity.BlogDescriptionUpdated) =
    updateIndexDoc(
      event.id,
      "description" -> event.description,
      "updatedAt"   -> event.updatedAt,
      "updatedBy"   -> event.updatedBy.code
    )

  def updateBlogCategory(event: BlogEntity.BlogCategoryUpdated) =
    updateIndexDoc(
      event.id,
      "categoryId" -> event.categoryId,
      "updatedAt"  -> event.updatedAt,
      "updatedBy"  -> event.updatedBy.code
    )

  def assignBlogAuthorPrincipal(event: BlogEntity.BlogAuthorPrincipalAssigned) = {
    val authorsField = alias2FieldName("authors")
    for {
      _ <- client.execute {
             updateById(indexName, event.id)
               .script(s"""ctx._source.${authorsField}.add("${event.principal.code}")""")
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

  def unassignBlogAuthorPrincipal(event: BlogEntity.BlogAuthorPrincipalUnassigned) = {
    val authorsField = alias2FieldName("authors")
    for {
      _ <- client.execute {
             updateById(indexName, event.id)
               .script(
                 s"""if (ctx._source.${authorsField}.contains("${event.principal.code}")) { ctx._source.${authorsField}.remove(ctx._source.${authorsField}.indexOf("${event.principal.code}")) }"""
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

  def assignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalAssigned) = {
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

  def unassignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalUnassigned) = {
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

  def activateBlog(event: BlogEntity.BlogActivated) =
    updateIndexDoc(
      event.id,
      "active"    -> true,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def deactivateBlog(event: BlogEntity.BlogDeactivated) =
    updateIndexDoc(
      event.id,
      "active"    -> false,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def deleteBlog(event: BlogEntity.BlogDeleted) =
    deleteIndexDoc(event.id)

  def findBlogs(query: BlogFindQuery): Future[FindResult] = {

    val filterQuery  = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "description" -> 1.0)
    )
    val blogIdsQuery =
      query.blogIds.map(blogIds => termsSetQuery(alias2FieldName("id"), blogIds, script("1"))).toSeq

    val categoryQuery = query.categories
      .map(categories => termsSetQuery(alias2FieldName("categoryId"), categories, script("1")))
      .toSeq
    val authorsQuery  = query.authors
      .map(authors => termsSetQuery(alias2FieldName("authors"), authors.map(_.code), script("1")))
      .toSeq
    val targetsQuery  = query.targets
      .map(targets => termsSetQuery(alias2FieldName("targets"), targets.map(_.code), script("1")))
      .toSeq
    val activeQuery   = query.active.map(matchQuery(alias2FieldName("active"), _)).toSeq

    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ blogIdsQuery ++ categoryQuery ++ authorsQuery ++ targetsQuery ++ activeQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

}
