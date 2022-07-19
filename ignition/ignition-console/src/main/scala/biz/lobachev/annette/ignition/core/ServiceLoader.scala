package biz.lobachev.annette.ignition.core

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.config.{ON_ERROR_IGNORE, ON_ERROR_STOP}
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.Try

trait ServiceLoader {
  val client: IgnitionLagomClient
  val config: Config
  val principal: AnnettePrincipal
  implicit val ec: ExecutionContext       = client.executionContext
  implicit val materializer: Materializer = client.materializer

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def createEntityLoader(entity: String, entityConfig: Config, principal: AnnettePrincipal): EntityLoader

  def run(): Future[Done] = {
    val entities = Try(config.getStringList("entities").asScala.toSeq).getOrElse(Seq.empty)
    val onError  = Try(config.getString("on-error")).getOrElse(ON_ERROR_IGNORE)
    Source(entities)
      .mapAsync(1) { entity =>
        val entityConfig = config.getConfig(entity)
        val entityLoader = createEntityLoader(entity, entityConfig, principal)
        val future       = entityLoader.run()
        if (onError == ON_ERROR_STOP) future
        else
          future.recover {
            case th =>
              log.error(s"Entity $entity failed ", th)
              Seq(Done)
          }
      }
      .runWith(Sink.ignore)
  }

}
