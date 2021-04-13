package biz.lobachev.annette.subscription.api.subscription

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.subscription_type.SubscriptionTypeId
import play.api.libs.json.Json

case class CreateSubscriptionPayload(
  subscriptionType: SubscriptionTypeId,
  objectId: ObjectId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object CreateSubscriptionPayload {
  implicit val format = Json.format[CreateSubscriptionPayload]
}
