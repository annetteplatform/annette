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

package biz.lobachev.annette.application.impl.application

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.impl.application.dao.{ApplicationDbDao, ApplicationIndexDao}
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.indexing.FindResult
import com.typesafe.config.Config
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ApplicationEntityService(
  clusterSharding: ClusterSharding,
  dbDao: ApplicationDbDao,
  indexDao: ApplicationIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: ApplicationId): EntityRef[ApplicationEntity.Command] =
    clusterSharding.entityRefFor(ApplicationEntity.typeKey, id)

  private def convertSuccess(confirmation: ApplicationEntity.Confirmation): Done =
    confirmation match {
      case ApplicationEntity.Success                 => Done
      case ApplicationEntity.ApplicationAlreadyExist => throw ApplicationAlreadyExist()
      case ApplicationEntity.ApplicationNotFound     => throw ApplicationNotFound()
      case _                                         => throw new RuntimeException("Match fail")
    }

  private def convertSuccessApplication(confirmation: ApplicationEntity.Confirmation): Application =
    confirmation match {
      case ApplicationEntity.SuccessApplication(application) => application
      case ApplicationEntity.ApplicationAlreadyExist         => throw ApplicationAlreadyExist()
      case ApplicationEntity.ApplicationNotFound             => throw ApplicationNotFound()
      case _                                                 => throw new RuntimeException("Match fail")
    }

  def createApplication(payload: CreateApplicationPayload): Future[Done] =
    refFor(payload.id)
      .ask[ApplicationEntity.Confirmation] { replyTo =>
        payload
          .into[ApplicationEntity.CreateApplication]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(convertSuccess)

  def updateApplication(payload: UpdateApplicationPayload): Future[Done] =
    refFor(payload.id)
      .ask[ApplicationEntity.Confirmation] { replyTo =>
        payload
          .into[ApplicationEntity.UpdateApplication]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(convertSuccess)

  def deleteApplication(payload: DeleteApplicationPayload): Future[Done] =
    refFor(payload.id)
      .ask[ApplicationEntity.Confirmation] { replyTo =>
        payload
          .into[ApplicationEntity.DeleteApplication]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(convertSuccess)

  def getApplication(id: ApplicationId, source: Option[String]): Future[Application] =
    if (DataSource.fromOrigin(source))
      refFor(id)
        .ask[ApplicationEntity.Confirmation](ApplicationEntity.GetApplication(id, _))
        .map(convertSuccessApplication)
    else
      dbDao
        .getApplication(id)
        .map(_.getOrElse(throw ApplicationNotFound()))

  def getApplications(ids: Set[ApplicationId], source: Option[String]): Future[Seq[Application]] =
    if (DataSource.fromOrigin(source))
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[ApplicationEntity.Confirmation](ApplicationEntity.GetApplication(id, _))
            .map {
              case ApplicationEntity.SuccessApplication(application) => Some(application)
              case _                                                 => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten)
    else
      dbDao.getApplications(ids)

  def getAllApplications(): Future[Seq[Application]] =
    dbDao.getAllApplications()

  def findApplications(query: FindApplicationQuery): Future[FindResult] =
    indexDao.findApplications(query)
}
