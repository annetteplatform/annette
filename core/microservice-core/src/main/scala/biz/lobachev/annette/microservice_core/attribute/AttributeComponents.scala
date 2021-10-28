package biz.lobachev.annette.microservice_core.attribute

import biz.lobachev.annette.core.attribute.AttributeMetadata

import scala.concurrent.Future

trait AttributeComponents {
  val entityMetadata: AttributeMetadataValidator

  def getEntityMetadata: Future[Map[String, AttributeMetadata]] =
    Future.successful(entityMetadata.metadata)

  protected def extractAttributes(withAttributes: Option[String]) =
    withAttributes match {
      case Some("all") => entityMetadata.metadata.keys.toSeq
      case Some("")    => Seq.empty
      case Some(list)  => list.split(",").toSeq
      case None        => Seq.empty
    }

  protected def splitAttributesByStorage(attributes: Seq[String]): (Seq[String], Seq[String]) = {
    val readSideAttributes  = attributes
      .filter(attr => entityMetadata.metadata.get(attr).map(_.readSidePersistence).getOrElse(false))
    val writeSideAttributes = (attributes.toSet -- readSideAttributes.toSet).toSeq
    (readSideAttributes, writeSideAttributes)
  }

}
