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

import biz.lobachev.annette.subscription.impl.subscription.dao.SubscriptionCassandraDbDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

private[impl] class SubscriptionDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: SubscriptionCassandraDbDao
) extends ReadSideProcessor[SubscriptionEntity.Event] {

  def buildHandler() =
    readSide
      .builder[SubscriptionEntity.Event]("Subscription_Subscription_CasEventOffset")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[SubscriptionEntity.SubscriptionCreated](e => dbDao.createSubscription(e.event))
      .setEventHandler[SubscriptionEntity.SubscriptionDeleted](e => dbDao.deleteSubscription(e.event))
      .build()

  def aggregateTags = SubscriptionEntity.Event.Tag.allTags

}
