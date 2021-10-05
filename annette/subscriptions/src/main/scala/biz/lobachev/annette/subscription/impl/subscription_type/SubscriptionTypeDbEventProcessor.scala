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

import biz.lobachev.annette.subscription.impl.subscription_type.dao.SubscriptionTypeCassandraDbDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.Future

private[impl] class SubscriptionTypeDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: SubscriptionTypeCassandraDbDao
) extends ReadSideProcessor[SubscriptionTypeEntity.Event] {

  def buildHandler() =
    readSide
      .builder[SubscriptionTypeEntity.Event]("subscriptionType-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[SubscriptionTypeEntity.SubscriptionTypeCreated](e => createSubscriptionType(e.event))
      .setEventHandler[SubscriptionTypeEntity.SubscriptionTypeUpdated](e => updateSubscriptionType(e.event))
      .setEventHandler[SubscriptionTypeEntity.SubscriptionTypeDeleted](e => deleteSubscriptionType(e.event))
      .build()

  def aggregateTags = SubscriptionTypeEntity.Event.Tag.allTags

  private def createSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeCreated) =
    Future.successful(dbDao.createSubscriptionType(event))

  private def updateSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeUpdated) =
    Future.successful(dbDao.updateSubscriptionType(event))

  private def deleteSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeDeleted) =
    Future.successful(dbDao.deleteSubscriptionType(event))

}
