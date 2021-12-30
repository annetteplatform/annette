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

package biz.lobachev.annette.subscription.impl.subscription_type.model

import biz.lobachev.annette.subscription.api.subscription_type.{
  CreateSubscriptionTypePayload,
  DeleteSubscriptionTypePayload,
  SubscriptionType,
  UpdateSubscriptionTypePayload
}
import biz.lobachev.annette.subscription.impl.subscription_type.SubscriptionTypeEntity
import biz.lobachev.annette.subscription.impl.subscription_type.SubscriptionTypeEntity._
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object SubscriptionTypeSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[SubscriptionType],
      JsonSerializer[SubscriptionTypeEntity],
      JsonSerializer[SubscriptionTypeState],
      JsonSerializer[CreateSubscriptionTypePayload],
      JsonSerializer[UpdateSubscriptionTypePayload],
      JsonSerializer[DeleteSubscriptionTypePayload],
      // responses
      JsonSerializer[Confirmation],
      JsonSerializer[Success.type],
      JsonSerializer[SuccessSubscriptionType],
      JsonSerializer[NotFound.type],
      JsonSerializer[AlreadyExist.type],
      // events
      JsonSerializer[SubscriptionTypeCreated],
      JsonSerializer[SubscriptionTypeUpdated],
      JsonSerializer[SubscriptionTypeDeleted]
    )
}
