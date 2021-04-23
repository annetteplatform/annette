package biz.lobachev.annette.blogs.impl.blog

import biz.lobachev.annette.blogs.impl.blog.dao.BlogElasticIndexDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class BlogIndexEventProcessor(
  readSide: CassandraReadSide,
  elasticRepository: BlogElasticIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[BlogEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[BlogEntity.Event] =
    readSide
      .builder[BlogEntity.Event]("Blogs_Blog_ElasticEventOffset")
      .setGlobalPrepare(elasticRepository.createEntityIndex)
      .setEventHandler[BlogEntity.BlogCreated](e => createBlog(e.event))
      .setEventHandler[BlogEntity.BlogNameUpdated](e => updateBlogName(e.event))
      .setEventHandler[BlogEntity.BlogDescriptionUpdated](e => updateBlogDescription(e.event))
      .setEventHandler[BlogEntity.BlogCategoryUpdated](e => updateBlogCategory(e.event))
      .setEventHandler[BlogEntity.BlogTargetPrincipalAssigned](e => assignBlogTargetPrincipal(e.event))
      .setEventHandler[BlogEntity.BlogTargetPrincipalUnassigned](e => unassignBlogTargetPrincipal(e.event))
      .setEventHandler[BlogEntity.BlogActivated](e => activateBlog(e.event))
      .setEventHandler[BlogEntity.BlogDeactivated](e => deactivateBlog(e.event))
      .setEventHandler[BlogEntity.BlogDeleted](e => deleteBlog(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[BlogEntity.Event]] = BlogEntity.Event.Tag.allTags

  def createBlog(event: BlogEntity.BlogCreated): Future[Seq[BoundStatement]] =
    elasticRepository
      .createBlog(event)
      .map(_ => Seq.empty)

  def updateBlogName(event: BlogEntity.BlogNameUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateBlogName(event)
      .map(_ => Seq.empty)

  def updateBlogDescription(event: BlogEntity.BlogDescriptionUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateBlogDescription(event)
      .map(_ => Seq.empty)

  def updateBlogCategory(event: BlogEntity.BlogCategoryUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateBlogCategory(event)
      .map(_ => Seq.empty)

  def assignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    elasticRepository
      .assignBlogTargetPrincipal(event)
      .map(_ => Seq.empty)

  def unassignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    elasticRepository
      .unassignBlogTargetPrincipal(event)
      .map(_ => Seq.empty)

  def activateBlog(event: BlogEntity.BlogActivated): Future[Seq[BoundStatement]] =
    elasticRepository
      .activateBlog(event)
      .map(_ => Seq.empty)

  def deactivateBlog(event: BlogEntity.BlogDeactivated): Future[Seq[BoundStatement]] =
    elasticRepository
      .deactivateBlog(event)
      .map(_ => Seq.empty)

  def deleteBlog(event: BlogEntity.BlogDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
      .deleteBlog(event)
      .map(_ => Seq.empty)

}
