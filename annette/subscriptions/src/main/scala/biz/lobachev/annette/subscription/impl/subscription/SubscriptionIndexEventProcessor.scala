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

package biz.lobachev.annette.subscription.impl.subscription

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.subscription.impl.subscription.dao.SubscriptionIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class SubscriptionIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: SubscriptionIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[SubscriptionEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[SubscriptionEntity.Event]("subscription-indexing")
      .setGlobalPrepare(() => indexDao.createEntityIndex())
      .setEventHandler[SubscriptionEntity.SubscriptionCreated](handle(indexDao.createSubscription))
      .setEventHandler[SubscriptionEntity.SubscriptionDeleted](handle(indexDao.deleteSubscription))
      .build()

  def aggregateTags = SubscriptionEntity.Event.Tag.allTags

}
