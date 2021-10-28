/*
 * Copyright 2016 Cameron McKay
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package biz.lobachev.annette.core.utils

object Encase {

  def toLowerCamel(str: String): String = convert(lowerCase, titleCase, "", str)

  def toUpperCamel(str: String): String = convert(titleCase, titleCase, "", str)

  def toLowerSnake(str: String): String = convert(lowerCase, lowerCase, "_", str)

  def toUpperSnake(str: String): String = convert(upperCase, upperCase, "_", str)

  def toLowerKebab(str: String): String = convert(lowerCase, lowerCase, "-", str)

  def toUpperKebab(str: String): String = convert(upperCase, upperCase, "-", str)

  private def titleCase = (_: String).toLowerCase.capitalize

  private def lowerCase = (_: String).toLowerCase

  private def upperCase = (_: String).toUpperCase

  def convert(headTransform: String => String, tailTransform: String => String, sep: String, str: String): String =
    (separate(str) match {
      case head :: tail => headTransform(head) :: tail.map(tailTransform)
      case list         => list
    }) mkString sep

  private val separatorPattern = List(
    "\\s+",
    "_",
    "-",
    "(?<=[A-Z])(?=[A-Z][a-z])",
    "(?<=[^A-Z_-])(?=[A-Z])",
    "(?<=[A-Za-z])(?=[^A-Za-z])"
  ).mkString("|").r

  def separate(str: String): List[String] = separatorPattern.split(str).toList

}
