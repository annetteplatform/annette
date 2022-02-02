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

package biz.lobachev.annette.bpm_repository.impl.bp

import biz.lobachev.annette.bpm_repository.api.bp.BusinessProcessFindQuery
import biz.lobachev.annette.bpm_repository.impl.db.{
  BpmRepositorySchema,
  BpmRepositorySchemaImplicits,
  BusinessProcessRecord
}
import biz.lobachev.annette.core.model.indexing.SortBy
import slick.jdbc.PostgresProfile.api._

object BusinessProcessQueries extends BpmRepositorySchemaImplicits {

  def getFilteredQuery(query: BusinessProcessFindQuery) =
    BpmRepositorySchema.businessProcesses.filter(rec =>
      List(
        query.filter
          .filter(_.trim.nonEmpty)
          .map(filter =>
            (rec.name like s"%$filter%") ||
              (rec.description like s"%$filter%")
          )
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    )

  def getSortedQuery(
    filteredQuery: Query[BpmRepositorySchema.BusinessProcessTable, BusinessProcessRecord, Seq],
    sortBy: SortBy
  ) =
    sortBy match {
      case SortBy("id", descending)   =>
        filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.id.desc else r.id.asc)
      case SortBy("name", descending) =>
        filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.name.desc else r.name.asc)
      case _                          => filteredQuery
    }

}
