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

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.subscription.api.subscription_type.{
  CreateSubscriptionTypePayload,
  DeleteSubscriptionTypePayload,
  SubscriptionType,
  SubscriptionTypeFindQuery,
  SubscriptionTypeId,
  UpdateSubscriptionTypePayload
}
import biz.lobachev.annette.subscription.api.subscription.{
  CreateSubscriptionPayload,
  DeleteSubscriptionPayload,
  ObjectId,
  Subscription,
  SubscriptionFindQuery,
  SubscriptionFindResult,
  SubscriptionKey
}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait SubscriptionServiceApi extends Service {

  def createSubscription: ServiceCall[CreateSubscriptionPayload, Done]
  def deleteSubscription: ServiceCall[DeleteSubscriptionPayload, Done]
  def getSubscription(fromReadSide: Boolean): ServiceCall[SubscriptionKey, Subscription]
  def getSubscriptions(fromReadSide: Boolean): ServiceCall[Set[SubscriptionKey], Set[Subscription]]
  def getSubscriptionsByPrincipals(
    subscriptionType: SubscriptionTypeId
  ): ServiceCall[Set[AnnettePrincipal], Set[SubscriptionKey]]
  def getSubscriptionsByObjects(
    subscriptionType: SubscriptionTypeId
  ): ServiceCall[Set[ObjectId], Set[SubscriptionKey]]
  def findSubscriptions: ServiceCall[SubscriptionFindQuery, SubscriptionFindResult]

  // org item subscriptionType

  def createSubscriptionType: ServiceCall[CreateSubscriptionTypePayload, Done]
  def updateSubscriptionType: ServiceCall[UpdateSubscriptionTypePayload, Done]
  def deleteSubscriptionType: ServiceCall[DeleteSubscriptionTypePayload, Done]
  def getSubscriptionType(id: SubscriptionTypeId, fromReadSide: Boolean): ServiceCall[NotUsed, SubscriptionType]
  def getSubscriptionTypes(
    fromReadSide: Boolean
  ): ServiceCall[Set[SubscriptionTypeId], Seq[SubscriptionType]]
  def findSubscriptionTypes: ServiceCall[SubscriptionTypeFindQuery, FindResult]

  final override def descriptor = {
    import Service._
    // @formatter:off
    named("subscriptions")
      .withCalls(
        pathCall("/api/subscriptions/v1/createSubscription",                             createSubscription),
        pathCall("/api/subscriptions/v1/deleteSubscription",                             deleteSubscription),
        pathCall("/api/subscriptions/v1/getSubscription/:fromReadSide",              getSubscription _),
        pathCall("/api/subscriptions/v1/getSubscriptions/:fromReadSide",             getSubscriptions _),
        pathCall("/api/subscriptions/v1/findSubscriptions",                              findSubscriptions),
        pathCall("/api/subscriptions/v1/getSubscriptionsByPrincipals/:subscriptionType", getSubscriptionsByPrincipals _),
        pathCall("/api/subscriptions/v1/getSubscriptionsByObjects/:subscriptionType",    getSubscriptionsByObjects _),

        pathCall("/api/subscriptions/v1/createSubscriptionType",                createSubscriptionType),
        pathCall("/api/subscriptions/v1/updateSubscriptionType",                updateSubscriptionType),
        pathCall("/api/subscriptions/v1/deleteSubscriptionType",                deleteSubscriptionType),
        pathCall("/api/subscriptions/v1/getSubscriptionType/:id/:readSide", getSubscriptionType _),
        pathCall("/api/subscriptions/v1/getSubscriptionTypes/:readSide",    getSubscriptionTypes _) ,
        pathCall("/api/subscriptions/v1/findSubscriptionTypes",                 findSubscriptionTypes),
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
    // @formatter:on
  }
}
