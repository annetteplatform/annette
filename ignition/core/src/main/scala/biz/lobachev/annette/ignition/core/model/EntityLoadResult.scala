package biz.lobachev.annette.ignition.core.model

case class EntityLoadResult(
  name: String,
  status: LoadStatus,
  quantity: Int,
  batches: Seq[BatchLoadResult]
) {
  def toStrings(): Seq[String] = {
    val res = for {
      batchResult <- batches
    } yield batchResult.toString()
    s"  $name $quantity $status" +: res
  }
}
