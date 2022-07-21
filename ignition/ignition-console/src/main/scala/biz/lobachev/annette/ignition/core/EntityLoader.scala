package biz.lobachev.annette.ignition.core

import akka.Done
import akka.pattern.CircuitBreakerOpenException
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.stream.{Materializer, RestartSettings}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.config.{MODE_UPSERT, ON_ERROR_IGNORE, ON_ERROR_STOP}
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Json, Reads}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

import java.net.ConnectException
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.io.{Source => FileSource}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait EntityLoader[A] {
  val config: Config
  val principal: AnnettePrincipal
  implicit val ec: ExecutionContext
  implicit val materializer: Materializer
  implicit val reads: Reads[A]

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def loadItem(item: A, mode: String): Future[Either[Throwable, Done.type]]

  def run(): Future[Done] = {
    val files       = Try(config.getStringList("data").asScala.toSeq).getOrElse(Seq.empty)
    val onError     = Try(config.getString("on-error")).getOrElse(ON_ERROR_IGNORE)
    val mode        = Try(config.getString("mode")).getOrElse(MODE_UPSERT)
    val parallelism = Try(config.getInt("parallelism")).getOrElse(1)

    Source(files)
      .mapAsync(1) { file =>
        val data   = loadFromFile(file)
        val future = saveData(data, onError, mode, parallelism)
          .map(n => log.info(s"Loaded from $file: success = ${n._1}, error = ${n._2}"))
        if (onError == ON_ERROR_STOP) future
        else
          future.recover {
            case th =>
              log.error(s"Load from $file failed ", th)
              Done
          }
      }
      .runWith(Sink.ignore)

  }

  def saveData(
    data: Seq[A],
    onError: String,
    mode: String,
    parallelism: Int
  ): Future[(Int, Int)] =
    Source(data)
      .mapAsync(parallelism) { item =>
        for {
          res <- loadWithBackOff(() =>
                   loadItem(item, mode).recoverWith {
                     case th: IllegalStateException       => Future.failed(th)
                     case th: ConnectException            => Future.failed(th)
                     case th: TimeoutException            => Future.failed(th)
                     case th: CircuitBreakerOpenException => Future.failed(th)
                     case th                              => Future.successful(Left(th))
                   }
                 )
        } yield res match {
          case Left(th) if onError == ON_ERROR_STOP => throw th
          case _                                    => res
        }
      }
      .runWith(Sink.seq)
      .map { seq =>
        val success = seq.count {
          case Left(_) => false
          case _       => true
        }
        val errors  = seq.count {
          case Left(_) => true
          case _       => false
        }
        (success, errors)
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

  def loadWithBackOff[T](fn: () => Future[T]): Future[T] =
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
