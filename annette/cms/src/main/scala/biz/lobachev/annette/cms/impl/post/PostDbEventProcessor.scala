package biz.lobachev.annette.cms.impl.post

import biz.lobachev.annette.cms.impl.post.dao.PostCassandraDbDao
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

private[impl] class PostDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: PostCassandraDbDao
) extends ReadSideProcessor[PostEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[PostEntity.Event] =
    readSide
      .builder[PostEntity.Event]("CMS_Post_CasEventOffset")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[PostEntity.PostCreated](e => dbDao.createPost(e.event))
      .setEventHandler[PostEntity.PostFeaturedUpdated](e => dbDao.updatePostFeatured(e.event))
      .setEventHandler[PostEntity.PostAuthorUpdated](e => dbDao.updatePostAuthor(e.event))
      .setEventHandler[PostEntity.PostTitleUpdated](e => dbDao.updatePostTitle(e.event))
      .setEventHandler[PostEntity.PostIntroUpdated](e => dbDao.updatePostIntro(e.event))
      .setEventHandler[PostEntity.PostContentUpdated](e => dbDao.updatePostContent(e.event))
      .setEventHandler[PostEntity.PostPublicationTimestampUpdated](e => dbDao.updatePostPublicationTimestamp(e.event))
      .setEventHandler[PostEntity.PostPublished](e => dbDao.publishPost(e.event))
      .setEventHandler[PostEntity.PostUnpublished](e => dbDao.unpublishPost(e.event))
      .setEventHandler[PostEntity.PostTargetPrincipalAssigned](e => dbDao.assignPostTargetPrincipal(e.event))
      .setEventHandler[PostEntity.PostTargetPrincipalUnassigned](e => dbDao.unassignPostTargetPrincipal(e.event))
      .setEventHandler[PostEntity.PostDeleted](e => dbDao.deletePost(e.event))
      .setEventHandler[PostEntity.PostMediaAdded](e => dbDao.addPostMedia(e.event))
      .setEventHandler[PostEntity.PostMediaRemoved](e => dbDao.removePostMedia(e.event))
      .setEventHandler[PostEntity.PostDocAdded](e => dbDao.addPostDoc(e.event))
      .setEventHandler[PostEntity.PostDocNameUpdated](e => dbDao.updatePostDocName(e.event))
      .setEventHandler[PostEntity.PostDocRemoved](e => dbDao.removePostDoc(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[PostEntity.Event]] = PostEntity.Event.Tag.allTags
}
