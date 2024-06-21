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

package biz.lobachev.annette.cms.impl.home_pages.dao

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.cms.api.home_pages.{HomePage, HomePageId}
import biz.lobachev.annette.cms.impl.home_pages.HomePageEntity
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}

class HomePageDbDao(
  override val session: CassandraSession
)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private val homePageSchema = quote(querySchema[HomePage]("home_pages"))

  private implicit val insertHomePageMeta = insertMeta[HomePage]()
  private implicit val updateHomePageMeta = updateMeta[HomePage](_.id)
  touch(insertHomePageMeta)
  touch(updateHomePageMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("home_pages")
               .column("id", Text, true)
               .column("application_id", Text)
               .column("principal", Text)
               .column("priority", Int)
               .column("page_id", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )

    } yield Done
  }

  def assignHomePage(event: HomePageEntity.HomePageAssigned) = {
    val homePageRecord = event
      .into[HomePage]
      .withFieldConst(_.id, HomePage.toCompositeId(event.applicationId, event.principal))
      .transform
    for {
      _ <- ctx.run(homePageSchema.insert(lift(homePageRecord)))
    } yield Done
  }

  def unassignHomePage(event: HomePageEntity.HomePageUnassigned) =
    for {
      _ <- ctx.run(
             homePageSchema
               .filter(_.id == lift(event.id))
               .delete
           )
    } yield Done

  def getHomePage(id: HomePageId): Future[Option[HomePage]] =
    for {
      maybeHomePageRecord <- ctx
                               .run(homePageSchema.filter(_.id == lift(id)))
                               .map(_.headOption)
    } yield maybeHomePageRecord

  def getHomePages(ids: Set[HomePageId]): Future[Seq[HomePage]] =
    for {
      homePages <- ctx
                     .run(homePageSchema.filter(b => liftQuery(ids).contains(b.id)))
    } yield homePages

}
