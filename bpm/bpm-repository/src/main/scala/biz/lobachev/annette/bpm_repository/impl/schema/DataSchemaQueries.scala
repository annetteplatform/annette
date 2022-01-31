package biz.lobachev.annette.bpm_repository.impl.schema

import biz.lobachev.annette.bpm_repository.api.schema.DataSchemaFindQuery
import biz.lobachev.annette.bpm_repository.impl.db.{BpmRepositorySchema, BpmRepositorySchemaImplicits, DataSchemaRecord}
import biz.lobachev.annette.core.model.indexing.SortBy
import slick.jdbc.PostgresProfile.api._

object DataSchemaQueries extends BpmRepositorySchemaImplicits {

  def getFilteredQuery(query: DataSchemaFindQuery) =
    BpmRepositorySchema.dataSchemas.filter(rec =>
      List(
        query.filter.map(filter =>
          (rec.name like s"%$filter%") ||
            (rec.description like s"%$filter%")
        )
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    )

  def getSortedQuery(filteredQuery: Query[BpmRepositorySchema.DataSchemaTable, DataSchemaRecord, Seq], sortBy: SortBy) =
    sortBy match {
      case SortBy("id", descending)   =>
        filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.id.desc else r.id.asc)
      case SortBy("name", descending) =>
        filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.name.desc else r.name.asc)
      case _                          => filteredQuery
    }

}
