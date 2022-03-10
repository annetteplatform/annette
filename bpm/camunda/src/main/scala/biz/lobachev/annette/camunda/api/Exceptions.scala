package biz.lobachev.annette.camunda.api

import biz.lobachev.annette.core.exception.{AnnetteTransportExceptionCompanion3, AnnetteTransportExceptionCompanion4}
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

object InvalidVariableValue extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.bpm.engine.invalidVariableValue"
  val Arg1Key: String = "type"
  val Arg2Key: String = "message"
  val Arg3Key: String = "payload"
}

object ProcessInstanceNotFound extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.bpm.engine.processInstance.notFound"
  val Arg1Key: String = "id"
  val Arg2Key: String = "message"
  val Arg3Key: String = "payload"
}

object ProcessInstanceVariableNotFound extends AnnetteTransportExceptionCompanion4 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.bpm.engine.processInstanceVariable.notFound"
  val Arg1Key: String = "id"
  val Arg2Key: String = "varName"
  val Arg3Key: String = "message"
  val Arg4Key: String = "payload"
}
