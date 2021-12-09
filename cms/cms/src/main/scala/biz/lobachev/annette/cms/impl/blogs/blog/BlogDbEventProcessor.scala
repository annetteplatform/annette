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

package biz.lobachev.annette.cms.impl.blogs.blog

import biz.lobachev.annette.cms.impl.blogs.blog.dao.BlogDbDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class BlogDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: BlogDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[BlogEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[BlogEntity.Event] =
    readSide
      .builder[BlogEntity.Event]("blog-cas")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[BlogEntity.BlogCreated](handle(dbDao.createBlog))
      .setEventHandler[BlogEntity.BlogNameUpdated](handle(dbDao.updateBlogName))
      .setEventHandler[BlogEntity.BlogDescriptionUpdated](handle(dbDao.updateBlogDescription))
      .setEventHandler[BlogEntity.BlogCategoryUpdated](handle(dbDao.updateBlogCategory))
      .setEventHandler[BlogEntity.BlogAuthorPrincipalAssigned](handle(dbDao.assignBlogAuthorPrincipal))
      .setEventHandler[BlogEntity.BlogAuthorPrincipalUnassigned](handle(dbDao.unassignBlogAuthorPrincipal))
      .setEventHandler[BlogEntity.BlogTargetPrincipalAssigned](handle(dbDao.assignBlogTargetPrincipal))
      .setEventHandler[BlogEntity.BlogTargetPrincipalUnassigned](handle(dbDao.unassignBlogTargetPrincipal))
      .setEventHandler[BlogEntity.BlogActivated](handle(dbDao.activateBlog))
      .setEventHandler[BlogEntity.BlogDeactivated](handle(dbDao.deactivateBlog))
      .setEventHandler[BlogEntity.BlogDeleted](handle(dbDao.deleteBlog))
      .build()

  def aggregateTags: Set[AggregateEventTag[BlogEntity.Event]] = BlogEntity.Event.Tag.allTags

}
