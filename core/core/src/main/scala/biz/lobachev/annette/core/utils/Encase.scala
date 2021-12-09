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
