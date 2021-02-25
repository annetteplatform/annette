package biz.lobachev.annette.ignition.core.model

case class ServiceLoadResult(
  name: String,
  status: LoadStatus,
  entities: Seq[EntityLoadResult]
) {
  def toStrings(): Seq[String] = {
    val res = (for {
      entityResult <- entities
    } yield entityResult.toStrings()).flatten
    s"$name $status" +: res
  }
}
