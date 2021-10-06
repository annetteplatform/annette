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

package biz.lobachev.annette.subscription.api
import akka.Done
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.subscription.api.subscription.{
  CreateSubscriptionPayload,
  DeleteSubscriptionPayload,
  ObjectId,
  Subscription,
  SubscriptionFindQuery,
  SubscriptionFindResult,
  SubscriptionKey
}
import biz.lobachev.annette.subscription.api.subscription_type._

import scala.concurrent.Future

trait SubscriptionService {

  def createSubscription(payload: CreateSubscriptionPayload): Future[Done]
  def deleteSubscription(payload: DeleteSubscriptionPayload): Future[Done]
  def getSubscriptionById(key: SubscriptionKey, fromReadSide: Boolean): Future[Subscription]
  def getSubscriptionsById(keys: Set[SubscriptionKey], fromReadSide: Boolean): Future[Set[Subscription]]
  def getSubscriptionsByPrincipals(
    principals: Set[AnnettePrincipal],
    subscriptionType: SubscriptionTypeId
  ): Future[Set[SubscriptionKey]]
  def getSubscriptionsByObjects(
    objectIds: Set[ObjectId],
    subscriptionType: SubscriptionTypeId
  ): Future[Set[SubscriptionKey]]
  def findSubscriptions(query: SubscriptionFindQuery): Future[SubscriptionFindResult]

  // subscriptionType methods

  def createSubscriptionType(payload: CreateSubscriptionTypePayload): Future[Done]
  def createOrUpdateSubscriptionType(payload: CreateSubscriptionTypePayload): Future[Done]
  def updateSubscriptionType(payload: UpdateSubscriptionTypePayload): Future[Done]
  def deleteSubscriptionType(payload: DeleteSubscriptionTypePayload): Future[Done]
  def getSubscriptionTypeById(id: SubscriptionTypeId, fromReadSide: Boolean): Future[SubscriptionType]
  def getSubscriptionTypesById(
    ids: Set[SubscriptionTypeId],
    fromReadSide: Boolean
  ): Future[Map[SubscriptionTypeId, SubscriptionType]]
  def findSubscriptionTypes(query: SubscriptionTypeFindQuery): Future[FindResult]
}
