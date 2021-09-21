import biz.lobachev.annette.microservice_core.indexing.{ConnectionConfig, IndexingProvider, IndexingRequestFailure}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.GetIndexResponse
import com.sksamuel.elastic4s.{RequestFailure, RequestSuccess, Response}
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse

implicit val ec = scala.concurrent.ExecutionContext.global

def processResponse[T]: PartialFunction[Response[T], T] = {
  case failure: RequestFailure    =>
    throw IndexingRequestFailure(failure.error.reason)
  case success: RequestSuccess[T] => success.result
}

val client    = IndexingProvider.createClient(
  ConnectionConfig(
    url = "https://localhost:9200",
    username = Some("admin"),
    password = Some("admin"),
    allowInsecure = true
  )
)
val indexName = "dev-persons-person"

val future1 = for {
  res <- client
           .execute(indexExists(indexName))
           .map(processResponse[IndexExistsResponse](_).exists)
} yield res

println(future1.await())

val future2 = for {
  res <- client.execute(getIndex(indexName)).map { response =>
           val result = processResponse[Map[String, GetIndexResponse]](response)(indexName)
           println(result)
           result
         }
} yield res

println(future2.await())
