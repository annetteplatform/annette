package biz.lobachev.annette.application.impl.application.dao

import biz.lobachev.annette.application.api.application.FindApplicationQuery
import biz.lobachev.annette.application.impl.application.ApplicationEntity
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.{must, search}
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ApplicationIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.application-index"

  def createApplication(event: ApplicationEntity.ApplicationCreated) =
    createIndexDoc(
      event.id,
      "id"        -> event.id,
      "name"      -> event.name,
      "updatedAt" -> event.createdAt
    )

  def updateApplicationName(event: ApplicationEntity.ApplicationNameUpdated) =
    updateIndexDoc(
      event.id,
      "name"      -> event.name,
      "updatedAt" -> event.updatedAt
    )

  def deleteApplication(event: ApplicationEntity.ApplicationDeleted) =
    deleteIndexDoc(event.id)

  def findApplications(query: FindApplicationQuery): Future[FindResult] = {
    val filterQuery            = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "id" -> 1.0)
    )
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)
  }

}
