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

package biz.lobachev.annette.org_structure.impl.hierarchy

import akka.Done
import akka.stream.scaladsl.Flow
import biz.lobachev.annette.attributes.api.AttributeServiceApi
import biz.lobachev.annette.attributes.api.index.{
  IndexAttributeAssigned,
  IndexAttributeCreated,
  IndexAttributeUnassigned,
  IndexEvent
}
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.HierarchyElasticIndexDao

import scala.concurrent.Future

class AttributeServiceSubscriber(attributeService: AttributeServiceApi, indexDao: HierarchyElasticIndexDao) {
  final val ORG_STRUCTURE_SCHEMA_IDS = Set("org-item", "org-position", "org-unit")

  attributeService.indexTopic.subscribe.atLeastOnce(Flow[IndexEvent].mapAsync(1) {
    case event: IndexAttributeCreated if ORG_STRUCTURE_SCHEMA_IDS.contains(event.id.schemaId)    =>
      indexDao.createAttribute(event.index)
    case event: IndexAttributeAssigned if ORG_STRUCTURE_SCHEMA_IDS.contains(event.id.schemaId)   =>
      indexDao.assignAttribute(event.objectId, event.fieldName, event.attribute)
    case event: IndexAttributeUnassigned if ORG_STRUCTURE_SCHEMA_IDS.contains(event.id.schemaId) =>
      indexDao.unassignAttribute(event.objectId, event.fieldName)
    case _                                                                                       => Future.successful(Done)
  })

}
