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

package biz.lobachev.annette.persons.impl.category

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

class CategoryDbEventProcessor(
  dbDao: dao.CategoryDbDao,
  projectionId: String
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[CategoryEntity.Event] {

  override val projectionName: String = projectionId
  override val tags: Seq[String] = Tagger.fromEventName[CategoryEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[CategoryEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: CategoryEntity.CategoryCreated => dbDao.createCategory(evt).map(_ => Done)
      case evt: CategoryEntity.CategoryUpdated => dbDao.updateCategory(evt).map(_ => Done)
      case evt: CategoryEntity.CategoryDeleted => dbDao.deleteCategory(evt).map(_ => Done)
      case _                                   => Future.successful(Done)
    }
}
