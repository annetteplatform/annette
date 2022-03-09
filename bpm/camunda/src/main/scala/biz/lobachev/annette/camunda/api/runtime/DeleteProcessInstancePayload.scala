package biz.lobachev.annette.camunda.api.runtime

import play.api.libs.json.Json

/**
 * @param id
 * @param skipCustomListeners If set to true, the custom listeners will be skipped.
 * @param skipIoMappings If set to true, the input/output mappings will be skipped.
 * @param skipSubprocesses If set to true, subprocesses related to deleted processes will be skipped.
 * @param failIfNotExists If set to false, the request will still be successful if the process id is not found.
 */
case class DeleteProcessInstancePayload(
  id: String,
  skipCustomListeners: Option[Boolean] = None,
  skipIoMappings: Option[Boolean] = None,
  skipSubprocesses: Option[Boolean] = None,
  failIfNotExists: Option[Boolean] = None
)

object DeleteProcessInstancePayload {
  implicit val format = Json.format[DeleteProcessInstancePayload]
}
