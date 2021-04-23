package biz.lobachev.annette.blogs.impl.post.dao

import akka.Done
import biz.lobachev.annette.blogs.api.post._
import biz.lobachev.annette.blogs.impl.post.PostEntity
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class PostCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createPostStatement: PreparedStatement                     = _
  private var updatePostFeaturedStatement: PreparedStatement             = _
  private var updatePostAuthorStatement: PreparedStatement               = _
  private var updatePostTitleStatement: PreparedStatement                = _
  private var updatePostIntroStatement: PreparedStatement                = _
  private var updatePostContentStatement: PreparedStatement              = _
  private var updatePostPublicationTimestampStatement: PreparedStatement = _
  private var publishPostStatement: PreparedStatement                    = _
  private var unpublishPostStatement: PreparedStatement                  = _
  private var assignPostTargetPrincipalStatement: PreparedStatement      = _
  private var unassignPostTargetPrincipalStatement: PreparedStatement    = _
  private var deletePostStatement: PreparedStatement                     = _
  private var addPostMediaStatement: PreparedStatement                   = _
  private var removePostMediaStatement: PreparedStatement                = _
  private var addPostDocStatement: PreparedStatement                     = _
  private var updatePostDocNameStatement: PreparedStatement              = _
  private var removePostDocStatement: PreparedStatement                  = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS ?????? (
                                        |          id               text PRIMARY KEY,
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] = {
    for {
      createPostStmt                     <- session.prepare(
                                              """
                            |
                            |""".stripMargin
                                            )
      updatePostFeaturedStmt             <- session.prepare(
                                              """
                                    |
                                    |""".stripMargin
                                            )
      updatePostAuthorStmt               <- session.prepare(
                                              """
                                  |
                                  |""".stripMargin
                                            )
      updatePostTitleStmt                <- session.prepare(
                                              """
                                 |
                                 |""".stripMargin
                                            )
      updatePostIntroStmt                <- session.prepare(
                                              """
                                 |
                                 |""".stripMargin
                                            )
      updatePostContentStmt              <- session.prepare(
                                              """
                                   |
                                   |""".stripMargin
                                            )
      updatePostPublicationTimestampStmt <- session.prepare(
                                              """
                                                |
                                                |""".stripMargin
                                            )
      publishPostStmt                    <- session.prepare(
                                              """
                             |
                             |""".stripMargin
                                            )
      unpublishPostStmt                  <- session.prepare(
                                              """
                               |
                               |""".stripMargin
                                            )
      assignPostTargetPrincipalStmt      <- session.prepare(
                                              """
                                           |
                                           |""".stripMargin
                                            )
      unassignPostTargetPrincipalStmt    <- session.prepare(
                                              """
                                             |
                                             |""".stripMargin
                                            )
      deletePostStmt                     <- session.prepare(
                                              """
                            |
                            |""".stripMargin
                                            )
      addPostMediaStmt                   <- session.prepare(
                                              """
                              |
                              |""".stripMargin
                                            )
      removePostMediaStmt                <- session.prepare(
                                              """
                                 |
                                 |""".stripMargin
                                            )
      addPostDocStmt                     <- session.prepare(
                                              """
                            |
                            |""".stripMargin
                                            )
      updatePostDocNameStmt              <- session.prepare(
                                              """
                                   |
                                   |""".stripMargin
                                            )
      removePostDocStmt                  <- session.prepare(
                                              """
                               |
                               |""".stripMargin
                                            )
    } yield {
      createPostStatement = createPostStmt
      updatePostFeaturedStatement = updatePostFeaturedStmt
      updatePostAuthorStatement = updatePostAuthorStmt
      updatePostTitleStatement = updatePostTitleStmt
      updatePostIntroStatement = updatePostIntroStmt
      updatePostContentStatement = updatePostContentStmt
      updatePostPublicationTimestampStatement = updatePostPublicationTimestampStmt
      publishPostStatement = publishPostStmt
      unpublishPostStatement = unpublishPostStmt
      assignPostTargetPrincipalStatement = assignPostTargetPrincipalStmt
      unassignPostTargetPrincipalStatement = unassignPostTargetPrincipalStmt
      deletePostStatement = deletePostStmt
      addPostMediaStatement = addPostMediaStmt
      removePostMediaStatement = removePostMediaStmt
      addPostDocStatement = addPostDocStmt
      updatePostDocNameStatement = updatePostDocNameStmt
      removePostDocStatement = removePostDocStmt
      Done
    }
  }

  def createPost(event: PostEntity.PostCreated): Future[Seq[BoundStatement]] =
    build(
      createPostStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updatePostFeatured(event: PostEntity.PostFeaturedUpdated): Future[Seq[BoundStatement]] =
    build(
      updatePostFeaturedStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updatePostAuthor(event: PostEntity.PostAuthorUpdated): Future[Seq[BoundStatement]] =
    build(
      updatePostAuthorStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updatePostTitle(event: PostEntity.PostTitleUpdated): Future[Seq[BoundStatement]] =
    build(
      updatePostTitleStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updatePostIntro(event: PostEntity.PostIntroUpdated): Future[Seq[BoundStatement]] =
    build(
      updatePostIntroStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updatePostContent(event: PostEntity.PostContentUpdated): Future[Seq[BoundStatement]] =
    build(
      updatePostContentStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updatePostPublicationTimestamp(event: PostEntity.PostPublicationTimestampUpdated): Future[Seq[BoundStatement]] =
    build(
      updatePostPublicationTimestampStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def publishPost(event: PostEntity.PostPublished): Future[Seq[BoundStatement]] =
    build(
      publishPostStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def unpublishPost(event: PostEntity.PostUnpublished): Future[Seq[BoundStatement]] =
    build(
      unpublishPostStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    build(
      assignPostTargetPrincipalStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    build(
      unassignPostTargetPrincipalStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def deletePost(event: PostEntity.PostDeleted): Future[Seq[BoundStatement]] =
    build(
      deletePostStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def addPostMedia(event: PostEntity.PostMediaAdded): Future[Seq[BoundStatement]] =
    build(
      addPostMediaStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.postId)
    )

  def removePostMedia(event: PostEntity.PostMediaRemoved): Future[Seq[BoundStatement]] =
    build(
      removePostMediaStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.postId)
    )

  def addPostDoc(event: PostEntity.PostDocAdded): Future[Seq[BoundStatement]] =
    build(
      addPostDocStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.postId)
    )

  def updatePostDocName(event: PostEntity.PostDocNameUpdated): Future[Seq[BoundStatement]] =
    build(
      updatePostDocNameStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.postId)
    )

  def removePostDoc(event: PostEntity.PostDocRemoved): Future[Seq[BoundStatement]] =
    build(
      removePostDocStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.postId)
    )

  def getPostById(id: PostId): Future[Option[Post]] =
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM ??? WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertPost))
    } yield maybeEntity

  def getPostAnnotationById(id: PostId): Future[Option[PostAnnotation]] =
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM ??? WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertPostAnnotation))
    } yield maybeEntity

  def getPostsById(ids: Set[PostId]): Future[Map[PostId, Post]]                     =
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM ??? WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertPost))
    } yield result.map(a => a.id -> a).toMap

  def getPostAnnotationsById(ids: Set[PostId]): Future[Map[PostId, PostAnnotation]] =
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM ??? WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertPostAnnotation))
    } yield result.map(a => a.id -> a).toMap

  private def convertPost(row: Row): Post                                           = ???
  private def convertPostAnnotation(row: Row): PostAnnotation                       = ???

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)
}
