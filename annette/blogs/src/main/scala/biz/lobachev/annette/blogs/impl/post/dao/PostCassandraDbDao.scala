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
  private var updatePublicationStatusStatement: PreparedStatement        = _
  private var assignPostTargetPrincipalStatement: PreparedStatement      = _
  private var unassignPostTargetPrincipalStatement: PreparedStatement    = _
  private var deletePostStatement: PreparedStatement                     = _
  private var deletePostTargetsStatement: PreparedStatement              = _
  private var deletePostMediaStatement: PreparedStatement                = _
  private var deletePostDocsStatement: PreparedStatement                 = _
  private var addPostMediaStatement: PreparedStatement                   = _
  private var removePostMediaStatement: PreparedStatement                = _
  private var addPostDocStatement: PreparedStatement                     = _
  private var updatePostDocNameStatement: PreparedStatement              = _
  private var removePostDocStatement: PreparedStatement                  = _
  private var updatePostTimestampStatement: PreparedStatement            = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS posts (
                                        |          id               text PRIMARY KEY,
                                        |          featured boolean,
                                        |          author_id_type text,
                                        |          author_id_id text,
                                        |          title text,
                                        |          intro_content_type text,
                                        |          intro_content text,
                                        |          content_type text,
                                        |          content text,
                                        |          publication_status text,
                                        |          publication_timestamp text,
                                        |          updated_at text,
                                        |          updated_by_type text,
                                        |          updated_by_id text,
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS post_targets (
                                        |          post_id text ,
                                        |          principal_type text,
                                        |          principal_id text,
                                        |          PRIMARY KEY (post_id, principal_type, principal_id)
                                        |)
                                        |""".stripMargin)

      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS post_media (
                                        |          post_id text ,
                                        |          media_id text,
                                        |          filename text,
                                        |          PRIMARY KEY (post_id, media_id)
                                        |)
                                        |""".stripMargin)

      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS post_docs (
                                        |          post_id text ,
                                        |          doc_id text,
                                        |          name text,
                                        |          filename text,
                                        |          PRIMARY KEY (post_id, doc_id)
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] = {
    for {
      createPostStmt                     <- session.prepare(
                                              """
                           INSERT INTO posts (id, featured, author_id_type, author_id_id, title,
                            |     intro_content_type, intro_content,
                            |     content_type, content,
                            |     publication_status,
                            |     updated_at, updated_by_type, updated_by_id
                            |     )
                            |   VALUES (:id, :featured, :author_id_type, :author_id_id, :title,
                            |     :intro_content_type, :intro_content,
                            |     :content_type, :content,
                            |     :publication_status,
                            |     :updated_at, :updated_by_type, :updated_by_id
                            |     )
                            |""".stripMargin
                                            )
      updatePostFeaturedStmt             <- session.prepare(
                                              """
                                    | UPDATE posts SET
                                    |   featured = :featured,
                                    |   updated_at = :updated_at,
                                    |   updated_by_type = :updated_by_type,
                                    |   updated_by_id = :updated_by_id
                                    |  WHERE id = :id
                                    |""".stripMargin
                                            )
      updatePostAuthorStmt               <- session.prepare(
                                              """
                                  | UPDATE posts SET
                                  |   author_id_type = :author_id_type,
                                  |   author_id_id = :author_id_id,
                                  |   updated_at = :updated_at,
                                  |   updated_by_type = :updated_by_type,
                                  |   updated_by_id = :updated_by_id
                                  |  WHERE id = :id
                                  |""".stripMargin
                                            )
      updatePostTitleStmt                <- session.prepare(
                                              """
                                 | UPDATE posts SET
                                 |   title = :title,
                                 |   updated_at = :updated_at,
                                 |   updated_by_type = :updated_by_type,
                                 |   updated_by_id = :updated_by_id
                                 |  WHERE id = :id
                                 |""".stripMargin
                                            )
      updatePostIntroStmt                <- session.prepare(
                                              """
                                 | UPDATE posts SET
                                 |   intro_content_type = :intro_content_type,
                                 |   intro_content = :intro_content,
                                 |   updated_at = :updated_at,
                                 |   updated_by_type = :updated_by_type,
                                 |   updated_by_id = :updated_by_id
                                 |  WHERE id = :id
                                 |""".stripMargin
                                            )
      updatePostContentStmt              <- session.prepare(
                                              """
                                   | UPDATE posts SET
                                   |   content_type = :content_type,
                                   |   content = :content,
                                   |   updated_at = :updated_at,
                                   |   updated_by_type = :updated_by_type,
                                   |   updated_by_id = :updated_by_id
                                   |  WHERE id = :id
                                   |""".stripMargin
                                            )
      updatePostPublicationTimestampStmt <- session.prepare(
                                              """
                                                | UPDATE posts SET
                                                |   publication_timestamp = :publication_timestamp,
                                                |   updated_at = :updated_at,
                                                |   updated_by_type = :updated_by_type,
                                                |   updated_by_id = :updated_by_id
                                                |  WHERE id = :id
                                                |""".stripMargin
                                            )
      updatePublicationStatusPostStmt    <- session.prepare(
                                              """
                                             | UPDATE posts SET
                                             |   publication_status = :publication_status,
                                             |   updated_at = :updated_at,
                                             |   updated_by_type = :updated_by_type,
                                             |   updated_by_id = :updated_by_id
                                             |  WHERE id = :id
                                             |""".stripMargin
                                            )

      assignPostTargetPrincipalStmt      <- session.prepare(
                                              """
                                           | INSERT INTO post_targets (post_id, principal_type, principal_id )
                                           |   VALUES (:post_id, :principal_type, :principal_id)
                                           |""".stripMargin
                                            )
      unassignPostTargetPrincipalStmt    <- session.prepare(
                                              """
                                             | DELETE FROM post_targets
                                             |   WHERE
                                             |     post_id = :post_id AND
                                             |     principal_type = :principal_type AND
                                             |     principal_id = :principal_id
                                             |""".stripMargin
                                            )
      deletePostStmt                     <- session.prepare(
                                              """
                            | DELETE FROM posts
                            |   WHERE id = :id
                            |""".stripMargin
                                            )
      deletePostTargetsStmt              <- session.prepare(
                                              """
                                   | DELETE FROM post_targets
                                   |   WHERE post_id = :post_id
                                   |""".stripMargin
                                            )
      deletePostMediaStmt                <- session.prepare(
                                              """
                                 | DELETE FROM post_media
                                 |   WHERE post_id = :post_id
                                 |""".stripMargin
                                            )
      deletePostDocsStmt                 <- session.prepare(
                                              """
                                | DELETE FROM post_docs
                                |   WHERE post_id = :post_id
                                |""".stripMargin
                                            )
      addPostMediaStmt                   <- session.prepare(
                                              """
                              | INSERT INTO post_media (post_id, media_id, filename )
                              |   VALUES (:post_id, :media_id, :filename )
                              |""".stripMargin
                                            )
      removePostMediaStmt                <- session.prepare(
                                              """
                                 | DELETE FROM post_media
                                 |   WHERE
                                 |     post_id = :post_id AND
                                 |     media_id = :media_id
                                 |""".stripMargin
                                            )
      addPostDocStmt                     <- session.prepare(
                                              """
                            | INSERT INTO post_docs (post_id, doc_id, name, filename )
                            |   VALUES (:post_id, :doc_id, :name, :filename )
                            |""".stripMargin
                                            )
      updatePostDocNameStmt              <- session.prepare(
                                              """
                                   | UPDATE post_docs SET
                                   |   name = :name
                                   |  WHERE
                                   |     post_id = :post_id AND
                                   |     doc_id = :doc_id
                                   |""".stripMargin
                                            )
      removePostDocStmt                  <- session.prepare(
                                              """
                               | DELETE FROM post_docs
                               |   WHERE
                               |     post_id = :post_id AND
                               |     doc_id = :doc_id
                               |""".stripMargin
                                            )

      updatePostTimestampStmt            <- session.prepare(
                                              """
                                     | UPDATE posts SET
                                     |   updated_at = :updated_at,
                                     |   updated_by_type = :updated_by_type,
                                     |   updated_by_id = :updated_by_id
                                     |  WHERE id = :id
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
      updatePublicationStatusStatement = updatePublicationStatusPostStmt
      assignPostTargetPrincipalStatement = assignPostTargetPrincipalStmt
      unassignPostTargetPrincipalStatement = unassignPostTargetPrincipalStmt
      deletePostStatement = deletePostStmt
      deletePostTargetsStatement = deletePostTargetsStmt
      deletePostMediaStatement = deletePostMediaStmt
      deletePostDocsStatement = deletePostDocsStmt
      addPostMediaStatement = addPostMediaStmt
      removePostMediaStatement = removePostMediaStmt
      addPostDocStatement = addPostDocStmt
      updatePostDocNameStatement = updatePostDocNameStmt
      removePostDocStatement = removePostDocStmt
      updatePostTimestampStatement = updatePostTimestampStmt
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
      updatePublicationStatusStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def unpublishPost(event: PostEntity.PostUnpublished): Future[Seq[BoundStatement]] =
    build(
      updatePublicationStatusStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    build(
      assignPostTargetPrincipalStatement
        .bind()
        // TODO: insert .set
        .setString("post_id", event.id),
      updatePostTimestampStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    build(
      unassignPostTargetPrincipalStatement
        .bind()
        // TODO: insert .set
        .setString("post_id", event.id),
      updatePostTimestampStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def deletePost(event: PostEntity.PostDeleted): Future[Seq[BoundStatement]] =
    build(
      deletePostStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id),
      deletePostTargetsStatement
        .bind()
        // TODO: insert .set
        .setString("post_id", event.id),
      deletePostMediaStatement
        .bind()
        // TODO: insert .set
        .setString("post_id", event.id),
      deletePostDocsStatement
        .bind()
        // TODO: insert .set
        .setString("post_id", event.id)
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
      stmt        <- session.prepare("SELECT * FROM posts WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertPost))
    } yield maybeEntity

  def getPostAnnotationById(id: PostId): Future[Option[PostAnnotation]] =
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM posts WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertPostAnnotation))
    } yield maybeEntity

  def getPostsById(ids: Set[PostId]): Future[Map[PostId, Post]]                     =
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM posts WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertPost))
    } yield result.map(a => a.id -> a).toMap

  def getPostAnnotationsById(ids: Set[PostId]): Future[Map[PostId, PostAnnotation]] =
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM posts WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertPostAnnotation))
    } yield result.map(a => a.id -> a).toMap

  private def convertPost(row: Row): Post                                           = ???
  private def convertPostAnnotation(row: Row): PostAnnotation                       = ???

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)
}
