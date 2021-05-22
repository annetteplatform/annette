package biz.lobachev.annette.cms.impl.post

import akka.Done
import biz.lobachev.annette.cms.impl.post.dao.PostElasticIndexDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class PostIndexEventProcessor(
  readSide: CassandraReadSide,
  elasticRepository: PostElasticIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[PostEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[PostEntity.Event] =
    readSide
      .builder[PostEntity.Event]("CMS_Post_ElasticEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[PostEntity.PostCreated](e => createPost(e.event))
      .setEventHandler[PostEntity.PostFeaturedUpdated](e => updatePostFeatured(e.event))
      .setEventHandler[PostEntity.PostAuthorUpdated](e => updatePostAuthor(e.event))
      .setEventHandler[PostEntity.PostTitleUpdated](e => updatePostTitle(e.event))
      .setEventHandler[PostEntity.PostIntroUpdated](e => updatePostIntro(e.event))
      .setEventHandler[PostEntity.PostContentUpdated](e => updatePostContent(e.event))
      .setEventHandler[PostEntity.PostPublicationTimestampUpdated](e => updatePostPublicationTimestamp(e.event))
      .setEventHandler[PostEntity.PostPublished](e => publishPost(e.event))
      .setEventHandler[PostEntity.PostUnpublished](e => unpublishPost(e.event))
      .setEventHandler[PostEntity.PostTargetPrincipalAssigned](e => assignPostTargetPrincipal(e.event))
      .setEventHandler[PostEntity.PostTargetPrincipalUnassigned](e => unassignPostTargetPrincipal(e.event))
      .setEventHandler[PostEntity.PostDeleted](e => deletePost(e.event))
      .setEventHandler[PostEntity.PostMediaAdded](e => addPostMedia(e.event))
      .setEventHandler[PostEntity.PostMediaRemoved](e => removePostMedia(e.event))
      .setEventHandler[PostEntity.PostDocAdded](e => addPostDoc(e.event))
      .setEventHandler[PostEntity.PostDocNameUpdated](e => updatePostDocName(e.event))
      .setEventHandler[PostEntity.PostDocRemoved](e => removePostDoc(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[PostEntity.Event]] = PostEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    elasticRepository.createEntityIndex()

  def createPost(event: PostEntity.PostCreated): Future[Seq[BoundStatement]] =
    elasticRepository
      .createPost(event)
      .map(_ => Seq.empty)

  def updatePostFeatured(event: PostEntity.PostFeaturedUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updatePostFeatured(event)
      .map(_ => Seq.empty)

  def updatePostAuthor(event: PostEntity.PostAuthorUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updatePostAuthor(event)
      .map(_ => Seq.empty)

  def updatePostTitle(event: PostEntity.PostTitleUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updatePostTitle(event)
      .map(_ => Seq.empty)

  def updatePostIntro(event: PostEntity.PostIntroUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updatePostIntro(event)
      .map(_ => Seq.empty)

  def updatePostContent(event: PostEntity.PostContentUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updatePostContent(event)
      .map(_ => Seq.empty)

  def updatePostPublicationTimestamp(event: PostEntity.PostPublicationTimestampUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updatePostPublicationTimestamp(event)
      .map(_ => Seq.empty)

  def publishPost(event: PostEntity.PostPublished): Future[Seq[BoundStatement]] =
    elasticRepository
      .publishPost(event)
      .map(_ => Seq.empty)

  def unpublishPost(event: PostEntity.PostUnpublished): Future[Seq[BoundStatement]] =
    elasticRepository
      .unpublishPost(event)
      .map(_ => Seq.empty)

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    elasticRepository
      .assignPostTargetPrincipal(event)
      .map(_ => Seq.empty)

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    elasticRepository
      .unassignPostTargetPrincipal(event)
      .map(_ => Seq.empty)

  def deletePost(event: PostEntity.PostDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
      .deletePost(event)
      .map(_ => Seq.empty)

  def addPostMedia(event: PostEntity.PostMediaAdded): Future[Seq[BoundStatement]] =
    elasticRepository
      .addPostMedia(event)
      .map(_ => Seq.empty)

  def removePostMedia(event: PostEntity.PostMediaRemoved): Future[Seq[BoundStatement]] =
    elasticRepository
      .removePostMedia(event)
      .map(_ => Seq.empty)

  def addPostDoc(event: PostEntity.PostDocAdded): Future[Seq[BoundStatement]] =
    elasticRepository
      .addPostDoc(event)
      .map(_ => Seq.empty)

  def updatePostDocName(event: PostEntity.PostDocNameUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updatePostDocName(event)
      .map(_ => Seq.empty)

  def removePostDoc(event: PostEntity.PostDocRemoved): Future[Seq[BoundStatement]] =
    elasticRepository
      .removePostDoc(event)
      .map(_ => Seq.empty)

}
