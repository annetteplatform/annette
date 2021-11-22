package biz.lobachev.annette.cms.gateway.files

import akka.http.scaladsl.model.headers.ByteRange

trait RangeHeader

case class SatisfiableRange(range: ByteRange) extends RangeHeader
case object UnsatisfiableRange                extends RangeHeader

object RangeHeader {
  val RangeSetPattern = """^bytes=[0-9,-]+""".r

  val RangePattern = """(\d*)-(\d*)""".r

  def apply(rangeHeader: String): RangeHeader =
    rangeHeader match {
      case RangeSetPattern() =>
        val firstRange = rangeHeader.split("=")(1).split(",").head
        firstRange match {
          case RangePattern(first, last) =>
            val firstByte = asOptionLong(first)
            val lastByte  = asOptionLong(last)
            (firstByte, lastByte) match {
              case (Some(first), Some(last)) => SatisfiableRange(ByteRange(first, last))
              case (Some(offset), None)      => SatisfiableRange(ByteRange.fromOffset(offset))
              case (None, Some(length))      => SatisfiableRange(ByteRange.suffix(length))
              case _                         => UnsatisfiableRange
            }
          case _                         => UnsatisfiableRange
        }
      case _                 => UnsatisfiableRange
    }

  private def asOptionLong(string: String) = if (string.isEmpty) None else Some(string.toLong)
}
