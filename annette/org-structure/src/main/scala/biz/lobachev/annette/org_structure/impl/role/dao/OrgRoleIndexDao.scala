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

package biz.lobachev.annette.org_structure.impl.role.dao

import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity.OrgRoleDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class OrgRoleIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.role-index"

  def createOrgRole(event: OrgRoleEntity.OrgRoleCreated) =
    createIndexDoc(
      event.id,
      "id"          -> event.id,
      "name"        -> event.name,
      "description" -> event.description,
      "updatedAt"   -> event.createdAt
    )

  def updateOrgRole(event: OrgRoleEntity.OrgRoleUpdated) =
    updateIndexDoc(
      event.id,
      "id"          -> event.id,
      "name"        -> event.name,
      "description" -> event.description,
      "updatedAt"   -> event.updatedAt
    )

  def deleteOrgRole(event: OrgRoleDeleted) =
    deleteIndexDoc(event.id)

  def findOrgRole(query: OrgRoleFindQuery): Future[FindResult] = {

    val fieldQuery             = Seq(
      query.name.map(matchQuery(alias2FieldName("name"), _)),
      query.description.map(matchQuery(alias2FieldName("description"), _))
    ).flatten
    val filterQuery            = buildFilterQuery(query.filter, Seq("name" -> 3.0, "description" -> 2.0))
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ fieldQuery)) // ++ fieldQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)
  }

}
