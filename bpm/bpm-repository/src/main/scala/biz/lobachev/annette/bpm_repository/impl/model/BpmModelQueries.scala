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

package biz.lobachev.annette.bpm_repository.impl.model

import biz.lobachev.annette.bpm_repository.api.domain.BpmModelId
import biz.lobachev.annette.bpm_repository.api.model.BpmModelFindQuery
import biz.lobachev.annette.bpm_repository.impl.db.{BpmModelRecord, BpmRepositorySchema, BpmRepositorySchemaImplicits}
import biz.lobachev.annette.core.model.indexing.SortBy
import slick.jdbc.PostgresProfile.api._

object BpmModelQueries extends BpmRepositorySchemaImplicits {

  def getBpmModelWithXmlQuery(id: BpmModelId) =
    BpmRepositorySchema.bpmModels.filter(_.id === id).result

  def getFilteredQuery(query: BpmModelFindQuery) =
    BpmRepositorySchema.bpmModels.filter(rec =>
      List(
        query.filter.map(filter =>
          (rec.name like s"%$filter%") ||
            (rec.description like s"%$filter%") ||
            (rec.code like s"%$filter%")
        ),
        query.notations.filter(_.nonEmpty).map(notations => rec.notation.inSet(notations))
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    )

  def getSortedQuery(filteredQuery: Query[BpmRepositorySchema.BpmModelTable, BpmModelRecord, Seq], sortBy: SortBy) =
    sortBy match {
      case SortBy("id", descending)       =>
        filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.id.desc else r.id.asc)
      case SortBy("code", descending)     =>
        filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.code.desc else r.code.asc)
      case SortBy("name", descending)     =>
        filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.name.desc else r.name.asc)
      case SortBy("notation", descending) =>
        filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.notation.desc else r.notation.asc)
      case _                              => filteredQuery
    }

}
