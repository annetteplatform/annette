package biz.lobachev.annette.subscription.api.subscription

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.subscription_type.SubscriptionTypeId
import play.api.libs.json.Json

case class SubscriptionKey(
  subscriptionType: SubscriptionTypeId,
  objectId: ObjectId,
  principal: AnnettePrincipal
)

object SubscriptionKey {
  implicit val format = Json.format[SubscriptionKey]
}
