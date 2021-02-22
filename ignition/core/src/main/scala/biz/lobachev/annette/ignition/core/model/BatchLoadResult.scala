package biz.lobachev.annette.ignition.core.model

case class BatchLoadResult(
  name: String,
  status: LoadStatus,
  quantity: Option[Int]
) {
  override def toString: String = {
    val quantityString = quantity.map(_.toString).getOrElse("")
    s"    $name $quantityString $status"
  }

}
