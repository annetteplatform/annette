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

package biz.lobachev.annette.cms.impl.pages.page.dao

import biz.lobachev.annette.cms.api.pages.page.{ContentTypes, PageFindQuery}
import biz.lobachev.annette.cms.impl.pages.page.PageEntity
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class PageIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.page-index"

  def createPage(event: PageEntity.PageCreated) =
    createIndexDoc(
      event.id,
      "id"                -> event.id,
      "spaceId"           -> event.spaceId,
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

  def updatePageFeatured(event: PageEntity.PageFeaturedUpdated) =
    updateIndexDoc(
      event.id,
      "featured"  -> event.featured,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def updatePageAuthor(event: PageEntity.PageAuthorUpdated) =
    updateIndexDoc(
      event.id,
      "authorId"  -> event.authorId.code,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def updatePageTitle(event: PageEntity.PageTitleUpdated) =
    updateIndexDoc(
      event.id,
      "title"     -> event.title,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def updatePageWidgetContent(event: PageEntity.PageWidgetContentUpdated) =
    updateIndexDoc(
      event.id,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def changeWidgetContentOrder(event: PageEntity.WidgetContentOrderChanged) =
    updateIndexDoc(
      event.id,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def deleteWidgetContent(event: PageEntity.WidgetContentDeleted) =
    updateIndexDoc(
      event.id,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def changePageIndex(event: PageEntity.PageIndexChanged) = {
    val alias = event.contentType match {
      case ContentTypes.Intro => "intro"
      case ContentTypes.Page  => "content"
    }
    updateIndexDoc(
      event.id,
      alias -> event.indexData,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )
  }

  def updatePagePublicationTimestamp(event: PageEntity.PagePublicationTimestampUpdated) =
    updateIndexDoc(
      event.id,
      "publicationTimestamp" -> event.publicationTimestamp.orNull,
      "updatedAt"            -> event.updatedAt,
      "updatedBy"            -> event.updatedBy.code
    )

  def publishPage(event: PageEntity.PagePublished) =
    updateIndexDoc(
      event.id,
      "publicationStatus"    -> "published",
      "publicationTimestamp" -> event.publicationTimestamp,
      "updatedAt"            -> event.updatedAt,
      "updatedBy"            -> event.updatedBy.code
    )

  def unpublishPage(event: PageEntity.PageUnpublished) =
    updateIndexDoc(
      event.id,
      "publicationStatus" -> "draft",
      "updatedAt"         -> event.updatedAt,
      "updatedBy"         -> event.updatedBy.code
    )

  def assignPageTargetPrincipal(event: PageEntity.PageTargetPrincipalAssigned) = {
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

  def unassignPageTargetPrincipal(event: PageEntity.PageTargetPrincipalUnassigned) = {
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

  def deletePage(event: PageEntity.PageDeleted) =
    deleteIndexDoc(event.id)

  def findPages(query: PageFindQuery): Future[FindResult] = {

    val filterQuery                   = buildFilterQuery(
      query.filter,
      Seq("title" -> 3.0, "intro" -> 2.0, "content" -> 1.0)
    )
    val pageIdsQuery                  = query.pageIds.map(pageIds => termsSetQuery(alias2FieldName("id"), pageIds, script("1"))).toSeq
    val spaceQuery                    = query.spaces.map(spaces => termsSetQuery(alias2FieldName("spaceId"), spaces, script("1"))).toSeq
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
          filterQuery ++ pageIdsQuery ++ spaceQuery ++ featuredQuery ++
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
