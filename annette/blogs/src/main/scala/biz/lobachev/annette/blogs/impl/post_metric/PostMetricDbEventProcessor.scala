package biz.lobachev.annette.blogs.impl.post_metric

import biz.lobachev.annette.blogs.impl.post_metric.dao.PostMetricCassandraDbDao
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

private[impl] class PostMetricDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: PostMetricCassandraDbDao
) extends ReadSideProcessor[PostMetricEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[PostMetricEntity.Event] =
    readSide
      .builder[PostMetricEntity.Event]("Blogs_PostMetric_CasEventOffset")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[PostMetricEntity.PostViewed](e => dbDao.viewPost(e.event))
      .setEventHandler[PostMetricEntity.PostLiked](e => dbDao.likePost(e.event))
      .setEventHandler[PostMetricEntity.PostMetricDeleted](e => dbDao.deletePostMetric(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[PostMetricEntity.Event]] = PostMetricEntity.Event.Tag.allTags

}
