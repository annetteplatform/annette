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

package biz.lobachev.annette.microservice_core.pekko.attribute.dao

import biz.lobachev.annette.core.attribute.AttributeValues
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.pekko.db.{CassandraQuillDao, CassandraTableBuilder}
import io.getquill.EntityQuery

import java.time.OffsetDateTime
import org.apache.pekko.Done
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}

import scala.collection.immutable.{Seq, Set}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Replaces `microservice_core.attribute.dao.CassandraQuillDaoWithAttributes`. Used by
 * services whose entities carry attribute maps (persons, org-structure, principal-groups,
 * application, service-catalog, cms).
 *
 * Differences from the Lagom variant:
 *  - extends `microservice_core.pekko.db.CassandraQuillDao` (Pekko variant) instead of
 *    `microservice_core.db.CassandraQuillDao`
 *  - uses Pekko streams (`org.apache.pekko.stream.scaladsl.{Source, Sink}`) instead of
 *    Akka streams
 *  - uses `org.apache.pekko.Done` instead of `akka.Done`
 *  - uses `session.execute(...)` (driver-3 Session) instead of Lagom's
 *    `session.executeCreateTable(...)` to create the attribute table
 *
 * The `ctx` is the Pekko-variant `CassandraAsyncContext` from
 * `microservice_core.pekko.db.CassandraQuillDao`.
 */
trait CassandraQuillDaoWithAttributes extends CassandraQuillDao {
  implicit val ec: ExecutionContext
  implicit val materializer: Materializer
  import ctx._

  protected val attributesSchema: Quoted[EntityQuery[AttributesRecord]]

  def createAttributeTable(tableName: String): Future[Done] = {
    import CassandraTableBuilder.types._
    val cql = CassandraTableBuilder(tableName)
      .column("id", Text)
      .column("attribute", Text)
      .column("value", Text)
      .column("updated_at", Timestamp)
      .column("updated_by", Text)
      .withPrimaryKey("id", "attribute")
      .build
    // The Pekko-variant CassandraQuillDao doesn't expose a Lagom-style
    // `session.executeCreateTable`; instead we use the Quill ctx's underlying session.
    // `ctx.session` is the driver-3 Session; executeDdl ... is sync but wrapped in Future
    // so the API matches the Lagom variant's signature.
    Future {
      ctx.session.execute(cql)
      Done
    }
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

  protected def getAttributes(id: PersonId, attributes: Seq[String]): Future[AttributeValues] =
    ctx
      .run(attributesSchema.filter(r => r.id == lift(id) && liftQuery(attributes).contains(r.attribute)))
      .map(_.map(_.toAttributeValue).toMap)

  protected def getAttributes(ids: Set[PersonId], attributes: Seq[String]): Future[Map[String, AttributeValues]] =
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
