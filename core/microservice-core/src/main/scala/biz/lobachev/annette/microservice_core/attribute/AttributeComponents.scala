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
