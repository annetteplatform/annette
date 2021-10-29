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

import biz.lobachev.annette.cms.impl.blogs.blog.dao.BlogIndexDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.{ExecutionContext}

private[impl] class BlogIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: BlogIndexDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[BlogEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[BlogEntity.Event] =
    readSide
      .builder[BlogEntity.Event]("blog-index")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[BlogEntity.BlogCreated](handle(indexDao.createBlog))
      .setEventHandler[BlogEntity.BlogNameUpdated](handle(indexDao.updateBlogName))
      .setEventHandler[BlogEntity.BlogDescriptionUpdated](handle(indexDao.updateBlogDescription))
      .setEventHandler[BlogEntity.BlogCategoryUpdated](handle(indexDao.updateBlogCategory))
      .setEventHandler[BlogEntity.BlogTargetPrincipalAssigned](handle(indexDao.assignBlogTargetPrincipal))
      .setEventHandler[BlogEntity.BlogTargetPrincipalUnassigned](handle(indexDao.unassignBlogTargetPrincipal))
      .setEventHandler[BlogEntity.BlogActivated](handle(indexDao.activateBlog))
      .setEventHandler[BlogEntity.BlogDeactivated](handle(indexDao.deactivateBlog))
      .setEventHandler[BlogEntity.BlogDeleted](handle(indexDao.deleteBlog))
      .build()

  def aggregateTags: Set[AggregateEventTag[BlogEntity.Event]] = BlogEntity.Event.Tag.allTags

}
