package biz.lobachev.annette.blogs.impl.blog

import biz.lobachev.annette.blogs.impl.blog.dao.BlogCassandraDbDao
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

private[impl] class BlogDbEventProcessor(
  readSide: CassandraReadSide,
  casDao: BlogCassandraDbDao
) extends ReadSideProcessor[BlogEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[BlogEntity.Event] =
    readSide
      .builder[BlogEntity.Event]("Blogs_Blog_CasEventOffset")
      .setGlobalPrepare(() => casDao.createTables())
      .setPrepare(_ => casDao.prepareStatements())
      .setEventHandler[BlogEntity.BlogCreated](e => casDao.createBlog(e.event))
      .setEventHandler[BlogEntity.BlogNameUpdated](e => casDao.updateBlogName(e.event))
      .setEventHandler[BlogEntity.BlogDescriptionUpdated](e => casDao.updateBlogDescription(e.event))
      .setEventHandler[BlogEntity.BlogCategoryUpdated](e => casDao.updateBlogCategory(e.event))
      .setEventHandler[BlogEntity.BlogTargetPrincipalAssigned](e => casDao.assignBlogTargetPrincipal(e.event))
      .setEventHandler[BlogEntity.BlogTargetPrincipalUnassigned](e => casDao.unassignBlogTargetPrincipal(e.event))
      .setEventHandler[BlogEntity.BlogActivated](e => casDao.activateBlog(e.event))
      .setEventHandler[BlogEntity.BlogDeactivated](e => casDao.deactivateBlog(e.event))
      .setEventHandler[BlogEntity.BlogDeleted](e => casDao.deleteBlog(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[BlogEntity.Event]] = BlogEntity.Event.Tag.allTags

}
