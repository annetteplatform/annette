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

package biz.lobachev.annette.application.impl.translation.model

import java.time.OffsetDateTime

import biz.lobachev.annette.application.api.language.LanguageId
import biz.lobachev.annette.application.api.translation.{
  TranslationBranch,
  TranslationId,
  TranslationTexts,
  TranslationTree
}
import biz.lobachev.annette.core.model.AnnettePrincipal
import play.api.libs.json.{JsObject, JsString, Json}

case class TranslationState(
  id: TranslationId,
  name: String,
  tree: TranslationTree,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {

  def updateName(name: String, principal: AnnettePrincipal, timestamp: OffsetDateTime): TranslationState =
    copy(
      name = name,
      updatedAt = timestamp,
      updatedBy = principal
    )

  def createNestedBranches(path: List[String]): TranslationTree =
    path match {
      case head :: Nil  => TranslationBranch(head)
      case head :: tail => TranslationBranch(head, Map(tail.head -> createNestedBranches(tail)))
      case _            => throw new RuntimeException("error")
    }

  def createBranch(id: TranslationId, principal: AnnettePrincipal, timestamp: OffsetDateTime): TranslationState = {
    def createBranchRec(path: List[String], item: TranslationTree): TranslationTree =
      item match {
        case TranslationTexts(_, _)                                                                    => throw new RuntimeException("error")
        case branch @ TranslationBranch(_, children) if path.nonEmpty && children.contains(path.head)  =>
          branch.copy(children = children + (path.head -> createBranchRec(path.tail, children(path.head))))
        case branch @ TranslationBranch(_, children) if path.nonEmpty && !children.contains(path.head) =>
          branch.copy(children = children + (path.head -> createNestedBranches(path)))
        case branch                                                                                    => branch
      }

    val path = id.split("\\.").toList.tail
    copy(
      tree = createBranchRec(path, tree),
      updatedBy = principal,
      updatedAt = timestamp
    )
  }

  def updateText(
    id: TranslationId,
    languageId: LanguageId,
    text: String,
    principal: AnnettePrincipal,
    timestamp: OffsetDateTime
  ): TranslationState = {
    def updateTextRec(
      path: List[String],
      item: TranslationTree,
      languageId: LanguageId,
      text: String
    ): TranslationTree =
      item match {
        case translationTexts: TranslationTexts if path.isEmpty                                           =>
          translationTexts.copy(
            texts = translationTexts.texts + (languageId -> text)
          )
        case branch @ TranslationBranch(_, children) if path.nonEmpty && children.contains(path.head)     =>
          branch.copy(children =
            children + (path.head -> updateTextRec(path.tail, children(path.head), languageId, text))
          )
        case branch @ TranslationBranch(_, children) if path.length == 1 && !children.contains(path.head) =>
          branch.copy(children =
            children + (path.head -> TranslationTexts(path.head, Map(languageId -> text)))
          )
        case _                                                                                            => throw new RuntimeException("error")
      }

    val path = id.split("\\.").toList.tail
    copy(
      tree = updateTextRec(path, tree, languageId, text),
      updatedAt = timestamp,
      updatedBy = principal
    )
  }

  def deleteItem(id: TranslationId, principal: AnnettePrincipal, timestamp: OffsetDateTime): TranslationState = {
    def deleteItemRec(
      path: List[String],
      item: TranslationTree
    ): TranslationTree =
      item match {
        case branch @ TranslationBranch(_, children) if path.length == 1 && children.contains(path.head) =>
          branch.copy(children = children - path.head)
        case branch @ TranslationBranch(_, children) if path.nonEmpty && children.contains(path.head)    =>
          branch.copy(
            children = children + (path.head -> deleteItemRec(path.tail, children(path.head)))
          )
        case _                                                                                           => throw new RuntimeException("error")
      }

    val path = id.split("\\.").toList.tail
    copy(
      tree = deleteItemRec(path, tree),
      updatedBy = principal,
      updatedAt = timestamp
    )
  }

  def deleteText(
    id: TranslationId,
    languageId: LanguageId,
    principal: AnnettePrincipal,
    timestamp: OffsetDateTime
  ): TranslationState = {
    def deleteTextRec(
      path: List[String],
      item: TranslationTree,
      languageId: LanguageId
    ): TranslationTree =
      item match {
        case translationTexts: TranslationTexts if path.isEmpty                                       =>
          translationTexts.copy(
            texts = translationTexts.texts - languageId
          )
        case branch @ TranslationBranch(_, children) if path.nonEmpty && children.contains(path.head) =>
          branch.copy(children =
            children + (path.head -> deleteTextRec(path.tail, children(path.head), languageId))
          )
        case _                                                                                        => throw new RuntimeException("error")
      }

    val path = id.split("\\.").toList.tail
    copy(
      tree = deleteTextRec(path, tree, languageId),
      updatedBy = principal,
      updatedAt = timestamp
    )
  }

  def languages(): Set[LanguageId] = languages(tree)

  def languages(item: TranslationTree): Set[LanguageId] =
    item match {
      case TranslationTexts(_, texts)     => texts.keySet
      case TranslationBranch(_, children) => children.values.toSet.flatMap(languages)
    }

  def json(languageId: LanguageId): JsObject = {
    def jsonRec(item: TranslationTree, languageId: LanguageId): JsObject =
      item match {
        case TranslationTexts(id, texts) if texts.contains(languageId) =>
          JsObject(Seq(id -> JsString(texts(languageId))))
        case TranslationTexts(_, _)                                    => JsObject(Seq())
        case TranslationBranch(id, children)                           =>
          JsObject(
            Seq(
              id -> children.values.foldLeft(JsObject.empty) { case (acc, child) => acc ++ jsonRec(child, languageId) }
            )
          )
      }
    jsonRec(tree, languageId)
  }

}

object TranslationState {
  def create(
    id: TranslationId,
    name: String,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime
  ): TranslationState = {
    val splitted = id.split("\\.")
    val app      = splitted(0)
    val service  = splitted(1)
    TranslationState(
      id = id,
      name = name,
      tree = TranslationBranch(app, Map(service -> TranslationBranch(service))),
      updatedBy = createdBy,
      updatedAt = createdAt
    )
  }
  implicit val format = Json.format[TranslationState]
}
