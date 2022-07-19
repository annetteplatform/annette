package biz.lobachev.annette.ignition.core

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.config.{MODE_UPSERT, ON_ERROR_IGNORE, ON_ERROR_STOP}
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsValue, Json}

import scala.io.{Source => FileSource}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait EntityLoader {
  val config: Config
  val principal: AnnettePrincipal
  implicit val ec: ExecutionContext
  implicit val materializer: Materializer

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def loadData(file: String, data: JsValue, onError: String, mode: String, parallelism: Int): Future[Int]

  def run(): Future[Done] = {
    val files       = Try(config.getStringList("data").asScala.toSeq).getOrElse(Seq.empty)
    val onError     = Try(config.getString("on-error")).getOrElse(ON_ERROR_IGNORE)
    val mode        = Try(config.getString("mode")).getOrElse(MODE_UPSERT)
    val parallelism = Try(config.getInt("parallelism")).getOrElse(1)

    Source(files)
      .mapAsync(1) { file =>
        val data   = getData("", file)
        val future = loadData(file, data, onError, mode, parallelism).map(n => log.info(s"Loaded $n items from $file"))
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

  protected def getData(name: String, filename: String): JsValue = {
    val jsonTry = Try(FileSource.fromResource(filename).mkString)
    jsonTry match {
      case Success(json) =>
        val resTry = Try(Json.parse(json))
        resTry match {
          case Success(value) => value
          case Failure(th)    =>
            val message = s"Parsing $name json failed: $filename"
            log.error(message, th)
            throw new IllegalArgumentException(message, th)
        }
      case Failure(th)   =>
        val message = s"$name file load failed: $filename"
        log.error(message, th)
        throw new IllegalArgumentException(message, th)
    }
  }

}
