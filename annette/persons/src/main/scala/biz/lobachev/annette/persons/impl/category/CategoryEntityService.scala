package biz.lobachev.annette.persons.impl.category

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.util.Timeout
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.persons.api.category._
import biz.lobachev.annette.persons.impl.category.dao.{CategoryDbDao, CategoryIndexDao}
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CategoryEntityService(
  clusterSharding: ClusterSharding,
  dbDao: CategoryDbDao,
  indexDao: CategoryIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val mat: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: PersonCategoryId): EntityRef[CategoryEntity.Command] =
    clusterSharding.entityRefFor(CategoryEntity.typeKey, id)

  private def convertSuccess(id: PersonCategoryId, confirmation: CategoryEntity.Confirmation): Done =
    confirmation match {
      case CategoryEntity.Success      => Done
      case CategoryEntity.NotFound     => throw PersonCategoryNotFound(id)
      case CategoryEntity.AlreadyExist => throw PersonCategoryAlreadyExist(id)
      case _                           => throw new RuntimeException("Match fail")
    }

  def createCategory(payload: CreateCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[CategoryEntity.Confirmation](CategoryEntity.CreateCategory(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[CategoryEntity.Confirmation](CategoryEntity.UpdateCategory(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[CategoryEntity.Confirmation](CategoryEntity.DeleteCategory(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getCategoryById(id: PersonCategoryId, fromReadSide: Boolean): Future[PersonCategory] =
    if (fromReadSide) getCategoryByIdFromReadSide(id)
    else getCategoryById(id)

  def getCategoryById(id: PersonCategoryId): Future[PersonCategory] =
    refFor(id)
      .ask[CategoryEntity.Confirmation](CategoryEntity.GetCategory(id, _))
      .map {
        case CategoryEntity.SuccessCategory(entity) => entity
        case _                                      => throw PersonCategoryNotFound(id)
      }

  def getCategoryByIdFromReadSide(id: PersonCategoryId): Future[PersonCategory] =
    for {
      maybeCategory <- dbDao.getCategoryById(id)
    } yield maybeCategory match {
      case Some(category) => category
      case None           => throw PersonCategoryNotFound(id)
    }

  def getCategoriesById(
    ids: Set[PersonCategoryId],
    fromReadSide: Boolean
  ): Future[Map[PersonCategoryId, PersonCategory]]                       =
    if (fromReadSide) dbDao.getCategoriesById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[CategoryEntity.Confirmation](CategoryEntity.GetCategory(id, _))
            .map {
              case CategoryEntity.SuccessCategory(entity) => Some(entity)
              case _                                      => None
            }
        }
        .map(seq => seq.flatten.map(category => category.id -> category).toMap)

  def findCategories(query: PersonCategoryFindQuery): Future[FindResult] =
    indexDao.findCategories(query)
}
