package biz.lobachev.annette.camunda4s.api.repository

import play.api.libs.json.Json

/**
 * @param id The id of the process definition  to be deleted.
 * @param cascade true, if all process instances, historic process instances and jobs for this deployment should be deleted.
 * @param skipCustomListeners true, if only the built-in ExecutionListeners should be notified with the end event.
 * @param skipIoMappings true, if all input/output mappings should not be invoked.
 */
case class DeleteProcessDefinitionPayload(
  id: String,
  cascade: Option[Boolean] = None,
  skipCustomListeners: Option[Boolean] = None,
  skipIoMappings: Option[Boolean] = None
)
object DeleteProcessDefinitionPayload {
  implicit val format = Json.format[DeleteProcessDefinitionPayload]
}
