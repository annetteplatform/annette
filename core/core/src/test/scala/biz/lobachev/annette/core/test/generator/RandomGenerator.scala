package biz.lobachev.annette.core.test.generator

import java.util.UUID

import scala.util.Random

trait RandomGenerator {

  def generateId: String = UUID.randomUUID().toString

  def generateWord(length: Int = 7): String =
    Random.alphanumeric
      .filter(_.isLetter)
      .take(length)
      .mkString

  def generateString(wordCount: Int = 7, wordLength: Int = 7) =
    (1 until wordCount)
      .map(_ => generateWord(wordLength))
      .mkString(" ")

  def generateSentence(wordCount: Int = 7, wordLength: Int = 7): String = {
    val str = generateString(wordCount, wordLength).toLowerCase
    s"${str.charAt(0).toUpper}${str.substring(1)}"
  }

  def generateText(sentenceCount: Int = 5, wordCount: Int = 7, wordLength: Int = 7) =
    (1 until sentenceCount)
      .map(_ => generateSentence(wordCount, wordLength))
      .mkString(". ") + "."

}
