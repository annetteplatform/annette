/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
