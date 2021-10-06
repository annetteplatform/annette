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

package biz.lobachev.annette.subscription.impl

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.subscription.api.SubscriptionServiceApi
import biz.lobachev.annette.subscription.api.subscription_type._
import biz.lobachev.annette.subscription.api.subscription.{
  CreateSubscriptionPayload,
  DeleteSubscriptionPayload,
  ObjectId,
  Subscription,
  SubscriptionFindQuery,
  SubscriptionFindResult,
  SubscriptionKey
}
import biz.lobachev.annette.subscription.impl.subscription_type.SubscriptionTypeEntityService
import biz.lobachev.annette.subscription.impl.subscription.SubscriptionEntityService
import com.lightbend.lagom.scaladsl.api.ServiceCall

class SubscriptionServiceApiImpl(
  subscriptionEntityService: SubscriptionEntityService,
  subscriptionTypeEntityService: SubscriptionTypeEntityService
) extends SubscriptionServiceApi {

  override def createSubscription: ServiceCall[CreateSubscriptionPayload, Done] =
    ServiceCall { payload =>
      subscriptionEntityService.createSubscription(payload)
    }

  override def deleteSubscription: ServiceCall[DeleteSubscriptionPayload, Done] =
    ServiceCall { payload =>
      subscriptionEntityService.deleteSubscription(payload)
    }

  override def getSubscriptionById(fromReadSide: Boolean): ServiceCall[SubscriptionKey, Subscription] =
    ServiceCall { key =>
      subscriptionEntityService.getSubscriptionById(key, fromReadSide)
    }

  override def getSubscriptionsById(fromReadSide: Boolean): ServiceCall[Set[SubscriptionKey], Set[Subscription]] =
    ServiceCall { keys =>
      subscriptionEntityService.getSubscriptionsById(keys, fromReadSide)
    }

  override def getSubscriptionsByPrincipals(
    subscriptionType: SubscriptionTypeId
  ): ServiceCall[Set[AnnettePrincipal], Set[SubscriptionKey]] =
    ServiceCall { principals =>
      subscriptionEntityService.getSubscriptionsByPrincipals(subscriptionType, principals)
    }

  override def getSubscriptionsByObjects(
    subscriptionType: SubscriptionTypeId
  ): ServiceCall[Set[ObjectId], Set[SubscriptionKey]] =
    ServiceCall { objectIds =>
      subscriptionEntityService.getSubscriptionsByObjects(subscriptionType, objectIds)
    }

  override def findSubscriptions: ServiceCall[SubscriptionFindQuery, SubscriptionFindResult] =
    ServiceCall { query =>
      subscriptionEntityService.findSubscriptions(query)
    }

  // ****************************** SubscriptionType methods ******************************

  override def createSubscriptionType: ServiceCall[CreateSubscriptionTypePayload, Done] =
    ServiceCall { payload =>
      subscriptionTypeEntityService.createSubscriptionType(payload)
    }

  override def updateSubscriptionType: ServiceCall[UpdateSubscriptionTypePayload, Done] =
    ServiceCall { payload =>
      subscriptionTypeEntityService.updateSubscriptionType(payload)
    }

  override def deleteSubscriptionType: ServiceCall[DeleteSubscriptionTypePayload, Done] =
    ServiceCall { payload =>
      subscriptionTypeEntityService.deleteSubscriptionType(payload)
    }

  override def getSubscriptionTypeById(
    id: SubscriptionTypeId,
    fromReadSide: Boolean
  ): ServiceCall[NotUsed, SubscriptionType] =
    ServiceCall { _ =>
      subscriptionTypeEntityService.getSubscriptionTypeById(id, fromReadSide)
    }

  override def getSubscriptionTypesById(
    fromReadSide: Boolean
  ): ServiceCall[Set[SubscriptionTypeId], Map[SubscriptionTypeId, SubscriptionType]] =
    ServiceCall { ids =>
      subscriptionTypeEntityService.getSubscriptionTypesById(ids, fromReadSide)
    }

  override def findSubscriptionTypes: ServiceCall[SubscriptionTypeFindQuery, FindResult] =
    ServiceCall { query =>
      subscriptionTypeEntityService.findSubscriptionTypes(query)
    }

}
