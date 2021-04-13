package biz.lobachev.annette.subscription.api.subscription

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.subscription_type.SubscriptionTypeId
import play.api.libs.json.Json

import java.time.OffsetDateTime

case class Subscription(
  subscriptionType: SubscriptionTypeId,
  objectId: ObjectId,
  principal: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
)

object Subscription {
  implicit val format = Json.format[Subscription]
}
