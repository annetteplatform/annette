package biz.lobachev.annette.persons.impl.category.dao

import akka.Done
import biz.lobachev.annette.core.model.category.{Category, CategoryId}
import biz.lobachev.annette.persons.impl.category.CategoryEntity

import scala.concurrent.Future

trait CategoryDbDao {

  def createTables(): Future[Done]
  def createCategory(event: CategoryEntity.CategoryCreated): Future[Done]

  def updateCategory(event: CategoryEntity.CategoryUpdated): Future[Done]

  def deleteCategory(event: CategoryEntity.CategoryDeleted): Future[Done]

  def getCategoryById(id: CategoryId): Future[Option[Category]]

  def getCategoriesById(ids: Set[CategoryId]): Future[Seq[Category]]

}
