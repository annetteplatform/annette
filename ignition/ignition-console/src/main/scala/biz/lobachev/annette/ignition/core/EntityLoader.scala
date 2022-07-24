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

package biz.lobachev.annette.ignition.core

import akka.pattern.CircuitBreakerOpenException
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.stream.{Materializer, RestartSettings}
import biz.lobachev.annette.ignition.core.config.{EntityLoaderConfig, StopOnError}
import biz.lobachev.annette.ignition.core.result._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Json, Reads}

import java.net.ConnectException
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.io.{Source => FileSource}
import scala.util.{Failure, Success, Try}

trait EntityLoader[A, C <: EntityLoaderConfig] {
  val name: String
  val config: C
  implicit val ec: ExecutionContext
  implicit val materializer: Materializer
  implicit val reads: Reads[A]

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def loadItem(item: A): Future[LoadStatus]

  def run(): Future[EntityLoadResult] =
    Source(config.data)
      .mapAsync(1) { file =>
        val data = loadFromFile(file)
        runBatch(file, data)
      }
      .takeWhile(
        {
          case BatchLoadResult(_, LoadFailed(_), _, _) if config.onError == StopOnError => false
          case _                                                                        => true
        },
        true
      )
      .runWith(Sink.seq)
      .map(seq =>
        if (config.onError == StopOnError)
          EntityLoadResult(name, seq.last.status, seq)
        else
          EntityLoadResult(name, LoadOk, seq)
      )

  def runBatch(
    name: String,
    data: Seq[A]
  ): Future[BatchLoadResult] =
    Source(data)
      .mapAsync(config.parallelism) { item =>
        for {
          res <- runWithBackOff(() =>
                   loadItem(item).recoverWith {
                     case th: IllegalStateException       => Future.failed(th)
                     case th: ConnectException            => Future.failed(th)
                     case th: TimeoutException            => Future.failed(th)
                     case th: CircuitBreakerOpenException => Future.failed(th)
                     case th                              => Future.successful(LoadFailed(th.getMessage))
                   }
                 )
        } yield res
      }
      .takeWhile(
        {
          case LoadFailed(msg) if config.onError == StopOnError =>
            log.error(s"Batch $name stopped due to error: $msg")
            false
          case _                                                => true
        },
        inclusive = true
      )
      .runWith(Sink.seq)
      .map { seq =>
        val success = seq.count {
          case LoadOk => false
          case _      => true
        }
        val errors  = seq.count {
          case LoadFailed(_) => true
          case _             => false
        }
        if (config.onError == StopOnError)
          BatchLoadResult(name, seq.last, success, errors)
        else
          BatchLoadResult(name, LoadOk, success, errors)
      }

  protected def loadFromFile(filename: String): Seq[A] = {

    val jsonTry = Try {
      val fileContent = FileSource.fromResource(filename).mkString
      if (filename.endsWith(".yaml") || filename.endsWith(".yml"))
        convertYamlToJson(fileContent)
      else fileContent
    }
    jsonTry match {
      case Success(json) =>
        val resTry = Try(Json.parse(json))
        resTry match {
          case Success(value) => value.as[Seq[A]]
          case Failure(th)    =>
            val message = s"Parsing json failed: $filename"
            log.error(message, th)
            throw new IllegalArgumentException(message, th)
        }
      case Failure(th)   =>
        val message = s"File load failed: $filename"
        log.error(message, th)
        throw new IllegalArgumentException(message, th)
    }
  }

  def convertYamlToJson(str: String): String = {
    val yamlReader = new ObjectMapper(new YAMLFactory())
    val obj        = yamlReader.readValue(str, classOf[Any])
    val jsonWriter = new ObjectMapper
    jsonWriter.writeValueAsString(obj)
  }

  def runWithBackOff[T](fn: () => Future[T]): Future[T] =
    RestartSource
      .onFailuresWithBackoff(
        RestartSettings(
          minBackoff = 3.seconds,
          maxBackoff = 20.seconds,
          randomFactor = 0.2
        )
          .withMaxRestarts(20, 3.seconds)
      ) { () =>
        Source.future(
          fn()
        )
      }
      .runWith(Sink.last)

}
