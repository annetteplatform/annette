package biz.lobachev.annette.ignition.core.attributes

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.attributes.api.schema.SchemaId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.ServiceLoader
import biz.lobachev.annette.ignition.core.model.EntityLoadResult
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class AttributeServiceLoader(
  schemaLoader: SchemaLoader,
  attributeDataLoader: AttributeDataLoader,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends ServiceLoader[AttributeIgnitionData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  override val name       = "Attribute"
  override val configName = "attributes"

  override protected def run(
    config: AttributeIgnitionData,
    principal: AnnettePrincipal
  ): Future[Seq[EntityLoadResult]] =
    Source(config.schemas)
      .mapAsync(1) { schema =>
        loadSchemaAndAssignment(schema, principal)
      }
      .mapConcat(_.toIterable)
      .runWith(Sink.seq)

  def loadSchemaAndAssignment(
    schema: SchemaIgnitionData,
    principal: AnnettePrincipal
  ): Future[Seq[EntityLoadResult]] = {
    val schemaId = SchemaId(schema.id, schema.sub)
    for {
      schemaResult <-
        schema.schemaFile
          .map(schemaFile => schemaLoader.loadFromFile(schemaId, schema.name, schemaFile, principal).map(Some(_)))
          .getOrElse(Future.successful(None))
      dataResult   <- if (schema.attrFiles.nonEmpty)
                        attributeDataLoader.loadFromFiles(schemaId, schema.name, schema.attrFiles, principal).map(Some(_))
                      else Future.successful(None)
    } yield Seq(schemaResult, dataResult).flatten
  }

}
