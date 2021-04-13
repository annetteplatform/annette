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
import biz.lobachev.annette.core.model.elastic.FindResult
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
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceImpl(api: SubscriptionServiceApi, implicit val ec: ExecutionContext)
    extends SubscriptionService {

  override def createSubscription(payload: CreateSubscriptionPayload): Future[Done] =
    api.createSubscription.invoke(payload)

  override def deleteSubscription(payload: DeleteSubscriptionPayload): Future[Done] =
    api.deleteSubscription.invoke(payload)

  override def getSubscriptionById(key: SubscriptionKey, fromReadSide: Boolean): Future[Subscription] =
    api.getSubscriptionById(fromReadSide).invoke(key)

  override def getSubscriptionsById(keys: Set[SubscriptionKey], fromReadSide: Boolean): Future[Set[Subscription]] =
    api.getSubscriptionsById(fromReadSide).invoke(keys)

  override def getSubscriptionsByPrincipals(
    principals: Set[AnnettePrincipal],
    subscriptionType: SubscriptionTypeId
  ): Future[Set[SubscriptionKey]] =
    api.getSubscriptionsByPrincipals(subscriptionType).invoke(principals)

  override def getSubscriptionsByObjects(
    objectIds: Set[ObjectId],
    subscriptionType: SubscriptionTypeId
  ): Future[Set[SubscriptionKey]] =
    api.getSubscriptionsByObjects(subscriptionType).invoke(objectIds)

  override def findSubscriptions(query: SubscriptionFindQuery): Future[SubscriptionFindResult] =
    api.findSubscriptions.invoke(query)

  // org subscriptionType methods

  def createSubscriptionType(payload: CreateSubscriptionTypePayload): Future[Done] =
    api.createSubscriptionType.invoke(payload)

  def updateSubscriptionType(payload: UpdateSubscriptionTypePayload): Future[Done] =
    api.updateSubscriptionType.invoke(payload)

  def createOrUpdateSubscriptionType(payload: CreateSubscriptionTypePayload): Future[Done] =
    createSubscriptionType(payload).recoverWith {
      case SubscriptionTypeAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateSubscriptionTypePayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updateSubscriptionType(updatePayload)
      case th                              => Future.failed(th)
    }

  def deleteSubscriptionType(payload: DeleteSubscriptionTypePayload): Future[Done] =
    api.deleteSubscriptionType.invoke(payload)

  def getSubscriptionTypeById(id: SubscriptionTypeId, fromReadSide: Boolean): Future[SubscriptionType] =
    api.getSubscriptionTypeById(id, fromReadSide).invoke()

  def getSubscriptionTypesById(
    ids: Set[SubscriptionTypeId],
    fromReadSide: Boolean
  ): Future[Map[SubscriptionTypeId, SubscriptionType]] =
    api.getSubscriptionTypesById(fromReadSide).invoke(ids)

  def findSubscriptionTypes(query: SubscriptionTypeFindQuery): Future[FindResult] =
    api.findSubscriptionTypes.invoke(query)

}
