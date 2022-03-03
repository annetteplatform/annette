package biz.lobachev.annette.camunda4s.test

import biz.lobachev.annette.camunda4s.models.ProcessDefinition
import cats.data.EitherT
import cats.effect.IO
import cats.effect.unsafe.implicits.{global => catsRuntime}
import cats.implicits._
import org.http4s.Method._
import org.http4s.blaze.client._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{DecodeFailure, EntityDecoder, Media, MediaType}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

class Camunda4sSpec extends AnyWordSpecLike with Matchers {

  "Camunda4s" should {

    "run http4s" in {
      val io = BlazeClientBuilder[IO].withExecutionContext(global).resource.use { client =>
        // use `client` here and return an `IO`.
        // the client will be acquired and shut down
        // automatically each time the `IO` is run.

        println("hello")

        val request = GET(
          uri"http://localhost:8080/engine-rest/engine/default/process-definition",
//          Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame")),
          Accept(MediaType.application.json)
        )
        val decoder = EntityDecoder.decodeBy(MediaType.application.json) { (m: Media[IO]) =>
          EitherT {
            m.as[String].map(s => Json.parse(s).as[Seq[ProcessDefinition]].asRight[DecodeFailure])
          }
        }
        client.expect[Seq[ProcessDefinition]](request)(decoder)
      }

      val future = io.unsafeToFuture()
      Await.result(future, Duration.Inf).foreach(println)
    }

  }
}
