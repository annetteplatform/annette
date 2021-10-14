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

package biz.lobachev.annette.subscription.impl.subscription_type

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.subscription.impl.subscription_type.dao.SubscriptionTypeIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class SubscriptionTypeIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: SubscriptionTypeIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[SubscriptionTypeEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[SubscriptionTypeEntity.Event]("subscriptionType-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[SubscriptionTypeEntity.SubscriptionTypeCreated](handle(indexDao.createSubscriptionType))
      .setEventHandler[SubscriptionTypeEntity.SubscriptionTypeUpdated](handle(indexDao.updateSubscriptionType))
      .setEventHandler[SubscriptionTypeEntity.SubscriptionTypeDeleted](handle(indexDao.deleteSubscriptionType))
      .build()

  def aggregateTags = SubscriptionTypeEntity.Event.Tag.allTags
}
