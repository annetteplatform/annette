package biz.lobachev.annette.ignition.core.model

sealed trait LoadStatus

case object LoadOk extends LoadStatus {
  override def toString: String = "Ok"
}

case class LoadFailed(message: String) extends LoadStatus {
  override def toString: String = s"Failed $message"
}
