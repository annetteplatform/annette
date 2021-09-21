package biz.lobachev.annette.microservice_core.test.indexing

import biz.lobachev.annette.microservice_core.indexing.{IndexingRequestFailure, _}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.GetIndexResponse
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.{RequestFailure, RequestSuccess, Response}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IndexDaoSpec extends AnyWordSpec with Matchers {

  implicit val ec = scala.concurrent.ExecutionContext.global

  val client    = IndexingProvider.createClient(
    ConnectionConfig(
      url = "https://localhost:9200",
      username = Some("admin"),
      password = Some("admin"),
      allowInsecure = true
    )
  )
  val indexName = "dev-persons-person"

  "elastic4s" should {

    "indexExists" in {
      val future = for {
        res <- client
                 .execute(indexExists(indexName))
                 .map(processResponse[IndexExistsResponse](_).exists)
      } yield res

      future.failed.foreach(th => th.printStackTrace())

      println(future.await)
      1 shouldBe 1
    }

    "getIndex" in {
      val future = for {
        res <- client.execute(getIndex(indexName)).map { response =>
                 val result = processResponse[Map[String, GetIndexResponse]](response)(indexName)
                 println(result)
                 result
               }
      } yield res

      future.failed.foreach(th => th.printStackTrace())

      val r = future.await
      println(r)
      1 shouldBe 1
    }
  }

  def processResponse[T]: PartialFunction[Response[T], T] = {
    case failure: RequestFailure    =>
      throw IndexingRequestFailure(failure.error.reason)
    case success: RequestSuccess[T] => success.result
  }

}
