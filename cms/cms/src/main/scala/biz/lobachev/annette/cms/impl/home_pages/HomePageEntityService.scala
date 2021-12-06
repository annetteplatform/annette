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

package biz.lobachev.annette.cms.impl.home_pages

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.cms.api.home_page.{
  AssignHomePagePayload,
  HomePage,
  HomePageFindQuery,
  HomePageId,
  HomePageNotFound,
  UnassignHomePagePayload
}
import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.cms.impl.home_pages.dao.{HomePageDbDao, HomePageIndexDao}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class HomePageEntityService(
  clusterSharding: ClusterSharding,
  dbDao: HomePageDbDao,
  indexDao: HomePageIndexDao
)(implicit
  ec: ExecutionContext
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(applicationId: String, principal: AnnettePrincipal): EntityRef[HomePageEntity.Command] = {
    val id = HomePage.toCompositeId(applicationId, principal)
    clusterSharding.entityRefFor(HomePageEntity.typeKey, id)
  }

  private def refFor(id: HomePageId): EntityRef[HomePageEntity.Command] =
    clusterSharding.entityRefFor(HomePageEntity.typeKey, id)

  private def convertSuccess(
    confirmation: HomePageEntity.Confirmation,
    applicationId: String,
    principal: AnnettePrincipal
  ): Done =
    confirmation match {
      case HomePageEntity.Success          => Done
      case HomePageEntity.HomePageNotFound => throw HomePageNotFound(applicationId, principal.code)
      case _                               => throw new RuntimeException("Match fail")
    }

  private def convertSuccessHomePage(
    confirmation: HomePageEntity.Confirmation,
    id: HomePageId
  ): HomePage = {
    val (applicationId, principal) = HomePage.fromCompositeId(id)
    confirmation match {
      case HomePageEntity.SuccessHomePage(homePage) => homePage
      case HomePageEntity.HomePageNotFound          => throw HomePageNotFound(applicationId, principal.code)
      case _                                        => throw new RuntimeException("Match fail")
    }
  }

  def assignHomePage(payload: AssignHomePagePayload): Future[Done] =
    refFor(payload.applicationId, payload.principal)
      .ask[HomePageEntity.Confirmation](replyTo =>
        payload
          .into[HomePageEntity.AssignHomePage]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.applicationId, payload.principal))

  def unassignHomePage(payload: UnassignHomePagePayload): Future[Done] =
    refFor(payload.applicationId, payload.principal)
      .ask[HomePageEntity.Confirmation](replyTo =>
        payload
          .into[HomePageEntity.UnassignHomePage]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.applicationId, payload.principal))

  private def getHomePage(id: HomePageId): Future[HomePage] =
    refFor(id)
      .ask[HomePageEntity.Confirmation](HomePageEntity.GetHomePage(id, _))
      .map(convertSuccessHomePage(_, id))

  def getHomePageById(id: HomePageId, fromReadSide: Boolean): Future[HomePage] =
    if (fromReadSide)
      dbDao
        .getHomePageById(id)
        .map(_.getOrElse {
          val (applicationId, principal) = HomePage.fromCompositeId(id)
          throw HomePageNotFound(applicationId, principal.code)
        })
    else
      getHomePage(id)

  def getHomePagesById(ids: Set[HomePageId], fromReadSide: Boolean): Future[Seq[HomePage]] =
    if (fromReadSide)
      dbDao.getHomePagesById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[HomePageEntity.Confirmation](HomePageEntity.GetHomePage(id, _))
            .map {
              case HomePageEntity.SuccessHomePage(homePage) => Some(homePage)
              case _                                        => None
            }
        }
        .map(_.flatten.toSeq)

  def getHomePageByPrincipalCodes(applicationId: String, principalCodes: Seq[String]): Future[PageId] =
    indexDao
      .getHomePageByPrincipalCodes(applicationId, principalCodes)
      .map(_.getOrElse(throw HomePageNotFound(applicationId, "")))

  def findHomePages(query: HomePageFindQuery): Future[FindResult] = indexDao.findHomePages(query)

}
