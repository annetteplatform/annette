package biz.lobachev.annette.camunda4s.api

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion3
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object BPMEngineError extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.bpm.engine.error"
  val Arg1Key: String = "type"
  val Arg2Key: String = "message"
  val Arg3Key: String = "payload"
}

object CreateDeploymentError extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.bpm.engine.deployment.createError"
  val Arg1Key: String = "type"
  val Arg2Key: String = "message"
  val Arg3Key: String = "payload"
}

object DeploymentNotFound extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.bpm.engine.deployment.notFound"
  val Arg1Key: String = "id"
  val Arg2Key: String = "message"
  val Arg3Key: String = "payload"
}

object ProcessDefinitionNotFoundById extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.bpm.engine.processDefinition.notFoundById"
  val Arg1Key: String = "id"
  val Arg2Key: String = "message"
  val Arg3Key: String = "payload"
}

object ProcessDefinitionNotFoundByKey extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.bpm.engine.processDefinition.notFoundByKey"
  val Arg1Key: String = "key"
  val Arg2Key: String = "message"
  val Arg3Key: String = "payload"
}
