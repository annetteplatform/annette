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

package biz.lobachev.annette.microservice_core.attribute.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.attribute.AttributeValues
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import io.getquill.EntityQuery

import java.time.OffsetDateTime
import scala.collection.immutable.{Seq, Set}
import scala.concurrent.{ExecutionContext, Future}

trait CassandraQuillDaoWithAttributes extends CassandraQuillDao {
  implicit val ec: ExecutionContext
  implicit val materializer: Materializer
  import ctx._

  protected val attributesSchema: Quoted[EntityQuery[AttributesRecord]]

  def createAttributeTable(tableName: String): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder(tableName)
               .column("id", Text)
               .column("attribute", Text)
               .column("value", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .withPrimaryKey("id", "attribute")
               .build
           )
    } yield Done
  }

  def updateAttributes(
    id: String,
    values: AttributeValues,
    updatedAt: OffsetDateTime,
    updatedBy: AnnettePrincipal
  ): Future[Done] =
    Source(values)
      .mapAsync(1) {
        case attribute -> value if value.length == 0 =>
          ctx
            .run(
              attributesSchema
                .filter(r =>
                  r.id == lift(id) &&
                    r.attribute == lift(attribute)
                )
                .delete
            )
            .map(_ => Done)
        case attribute -> value                      =>
          ctx
            .run(
              attributesSchema.insert(
                lift(
                  AttributesRecord(id, attribute, value, updatedAt, updatedBy)
                )
              )
            )
            .map(_ => Done)
      }
      .runWith(Sink.ignore)

  def deleteAttributes(id: String): Future[Done] =
    ctx
      .run(
        attributesSchema
          .filter(r => r.id == lift(id))
          .delete
      )
      .map(_ => Done)

  protected def getAttributesById(id: PersonId, attributes: Seq[String]): Future[AttributeValues] =
    ctx
      .run(attributesSchema.filter(r => r.id == lift(id) && liftQuery(attributes).contains(r.attribute)))
      .map(_.map(_.toAttributeValue).toMap)

  protected def getAttributesById(ids: Set[PersonId], attributes: Seq[String]): Future[Map[String, AttributeValues]] =
    ctx
      .run(
        attributesSchema.filter(r =>
          liftQuery(ids).contains(r.id) &&
            liftQuery(attributes).contains(r.attribute)
        )
      )
      .map(
        _.groupBy(_.id).map {
          case id -> recordSeq =>
            id -> recordSeq.map(attr => attr.attribute -> attr.value).toMap
        }
      )

}
