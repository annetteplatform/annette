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

package biz.lobachev.annette.cms.impl.post.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.cms.impl.post.PostEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

import java.time.OffsetDateTime
import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class PostCassandraDbDao(
  session: CassandraSession
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

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
  private var deletePostTargetsStatement: PreparedStatement              = _
  private var deletePostMediaStatement: PreparedStatement                = _
  private var deletePostDocsStatement: PreparedStatement                 = _
  private var addPostMediaStatement: PreparedStatement                   = _
  private var removePostMediaStatement: PreparedStatement                = _
  private var addPostDocStatement: PreparedStatement                     = _
  private var updatePostDocNameStatement: PreparedStatement              = _
  private var removePostDocStatement: PreparedStatement                  = _
  private var updatePostTimestampStatement: PreparedStatement            = _
  private var viewPostStatement: PreparedStatement                       = _
  private var likePostStatement: PreparedStatement                       = _
  private var unlikePostStatement: PreparedStatement                     = _
  private var deletePostViewsStatement: PreparedStatement                = _
  private var deletePostLikesStatement: PreparedStatement                = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS posts (
                                        |          id               text PRIMARY KEY,
                                        |          space_id text,
                                        |          featured boolean,
                                        |          author_id_type text,
                                        |          author_id_id text,
                                        |          title text,
                                        |          intro_content text,
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
                                        |          principal text,
                                        |          principal_type text,
                                        |          principal_id text,
                                        |          PRIMARY KEY (post_id, principal)
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
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS post_likes (
                                        |          post_id text,
                                        |          principal text,
                                        |          PRIMARY KEY (post_id, principal)
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS post_views (
                                        |          post_id text,
                                        |          principal text,
                                        |          views counter,
                                        |          PRIMARY KEY (post_id, principal)
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] = {
    for {
      createPostStmt                     <- session.prepare(
                                              """
                           INSERT INTO posts (id, space_id, featured, author_id_type, author_id_id, title,
                            |     intro_content,  content,
                            |     publication_status,
                            |     updated_at, updated_by_type, updated_by_id
                            |     )
                            |   VALUES (:id, :space_id, :featured, :author_id_type, :author_id_id, :title,
                            |     :intro_content, :content,
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
      publishPostStmt                    <- session.prepare(
                                              """
                             | UPDATE posts SET
                             |   publication_status = :publication_status,
                             |   publication_timestamp = :publication_timestamp,
                             |   updated_at = :updated_at,
                             |   updated_by_type = :updated_by_type,
                             |   updated_by_id = :updated_by_id
                             |  WHERE id = :id
                             |""".stripMargin
                                            )
      unpublishPostStmt                  <- session.prepare(
                                              """
                               | UPDATE posts SET
                               |   publication_status = :publication_status,
                               |   updated_at = :updated_at,
                               |   updated_by_type = :updated_by_type,
                               |   updated_by_id = :updated_by_id
                               |  WHERE id = :id
                               |""".stripMargin
                                            )

      assignPostTargetPrincipalStmt      <-
        session.prepare(
          """
            | INSERT INTO post_targets (post_id, principal,  principal_type, principal_id )
            |   VALUES (:post_id, :principal, :principal_type, :principal_id)
            |""".stripMargin
        )
      unassignPostTargetPrincipalStmt    <- session.prepare(
                                              """
                                             | DELETE FROM post_targets
                                             |   WHERE
                                             |     post_id = :post_id AND
                                             |     principal = :principal
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

      deletePostViewsStmt                <- session.prepare(
                                              """
                                 | DELETE FROM post_views
                                 |   WHERE post_id = :post_id
                                 |""".stripMargin
                                            )
      deletePostLikesStmt                <- session.prepare(
                                              """
                                 | DELETE FROM post_likes
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
      viewPostStmt                       <- session.prepare(
                                              """
                          | UPDATE post_views SET
                          |     views = views + 1
                          |   WHERE
                          |      post_id = :post_id AND
                          |      principal = :principal
                          |""".stripMargin
                                            )
      likePostStmt                       <- session.prepare(
                                              """
                          | INSERT INTO post_likes (post_id, principal)
                          |   VALUES (:post_id, :principal)
                          |""".stripMargin
                                            )
      unlikePostStmt                     <- session.prepare(
                                              """
                            | DELETE FROM post_likes
                            |   WHERE
                            |     post_id = :post_id AND
                            |      principal = :principal
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
      deletePostTargetsStatement = deletePostTargetsStmt
      deletePostMediaStatement = deletePostMediaStmt
      deletePostDocsStatement = deletePostDocsStmt
      addPostMediaStatement = addPostMediaStmt
      removePostMediaStatement = removePostMediaStmt
      addPostDocStatement = addPostDocStmt
      updatePostDocNameStatement = updatePostDocNameStmt
      removePostDocStatement = removePostDocStmt
      updatePostTimestampStatement = updatePostTimestampStmt
      viewPostStatement = viewPostStmt
      likePostStatement = likePostStmt
      unlikePostStatement = unlikePostStmt
      deletePostViewsStatement = deletePostViewsStmt
      deletePostLikesStatement = deletePostLikesStmt
      Done
    }
  }

  def createPost(event: PostEntity.PostCreated): Future[Seq[BoundStatement]] = {
    val targets = event.targets
      .map(target =>
        assignPostTargetPrincipalStatement
          .bind()
          .setString("post_id", event.id)
          .setString("principal", target.code)
          .setString("principal_type", target.principalType)
          .setString("principal_id", target.principalId)
      )
      .toSeq
    Future.successful(
      createPostStatement
        .bind()
        .setString("id", event.id)
        .setString("space_id", event.spaceId)
        .setBool("featured", event.featured)
        .setString("author_id_type", event.authorId.principalType)
        .setString("author_id_id", event.authorId.principalId)
        .setString("title", event.title)
        .setString("intro_content", Json.toJson(event.introContent).toString())
        .setString("content", Json.toJson(event.content).toString())
        .setString("publication_status", PublicationStatus.Draft.toString)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
        +: targets
    )
  }

  def updatePostFeatured(event: PostEntity.PostFeaturedUpdated): Future[Seq[BoundStatement]] =
    execute(
      updatePostFeaturedStatement
        .bind()
        .setString("id", event.id)
        .setBool("featured", event.featured)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updatePostAuthor(event: PostEntity.PostAuthorUpdated): Future[Seq[BoundStatement]] =
    execute(
      updatePostAuthorStatement
        .bind()
        .setString("id", event.id)
        .setString("author_id_type", event.authorId.principalType)
        .setString("author_id_id", event.authorId.principalId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updatePostTitle(event: PostEntity.PostTitleUpdated): Future[Seq[BoundStatement]] =
    execute(
      updatePostTitleStatement
        .bind()
        .setString("id", event.id)
        .setString("title", event.title)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updatePostIntro(event: PostEntity.PostIntroUpdated): Future[Seq[BoundStatement]] =
    execute(
      updatePostIntroStatement
        .bind()
        .setString("id", event.id)
        .setString("intro_content", Json.toJson(event.introContent).toString())
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updatePostContent(event: PostEntity.PostContentUpdated): Future[Seq[BoundStatement]] =
    execute(
      updatePostContentStatement
        .bind()
        .setString("id", event.id)
        .setString("content", Json.toJson(event.content).toString())
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updatePostPublicationTimestamp(event: PostEntity.PostPublicationTimestampUpdated): Future[Seq[BoundStatement]] =
    execute(
      updatePostPublicationTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("publication_timestamp", event.publicationTimestamp.map(_.toString).getOrElse(null))
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def publishPost(event: PostEntity.PostPublished): Future[Seq[BoundStatement]] =
    execute(
      publishPostStatement
        .bind()
        .setString("id", event.id)
        .setString("publication_status", "published")
        .setString("publication_timestamp", event.publicationTimestamp.toString)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def unpublishPost(event: PostEntity.PostUnpublished): Future[Seq[BoundStatement]] =
    execute(
      unpublishPostStatement
        .bind()
        .setString("id", event.id)
        .setString("publication_status", "draft")
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    execute(
      assignPostTargetPrincipalStatement
        .bind()
        .setString("post_id", event.id)
        .setString("principal", event.principal.code)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updatePostTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    execute(
      unassignPostTargetPrincipalStatement
        .bind()
        .setString("post_id", event.id)
        .setString("principal", event.principal.code),
      updatePostTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deletePost(event: PostEntity.PostDeleted): Future[Seq[BoundStatement]] =
    for {
      _ <- session.executeWrite(
             deletePostViewsStatement
               .bind()
               .setString("post_id", event.id)
           )
    } yield Seq(
      deletePostStatement
        .bind()
        .setString("id", event.id),
      deletePostTargetsStatement
        .bind()
        .setString("post_id", event.id),
      deletePostMediaStatement
        .bind()
        .setString("post_id", event.id),
      deletePostDocsStatement
        .bind()
        .setString("post_id", event.id),
      deletePostLikesStatement
        .bind()
        .setString("post_id", event.id)
    )

  def addPostMedia(event: PostEntity.PostMediaAdded): Future[Seq[BoundStatement]] =
    execute(
      addPostMediaStatement
        .bind()
        .setString("post_id", event.postId)
        .setString("media_id", event.mediaId)
        .setString("filename", event.filename),
      updatePostTimestampStatement
        .bind()
        .setString("id", event.postId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def removePostMedia(event: PostEntity.PostMediaRemoved): Future[Seq[BoundStatement]] =
    execute(
      removePostMediaStatement
        .bind()
        .setString("post_id", event.postId)
        .setString("media_id", event.mediaId),
      updatePostTimestampStatement
        .bind()
        .setString("id", event.postId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def addPostDoc(event: PostEntity.PostDocAdded): Future[Seq[BoundStatement]] =
    execute(
      addPostDocStatement
        .bind()
        .setString("post_id", event.postId)
        .setString("doc_id", event.docId)
        .setString("name", event.name)
        .setString("filename", event.filename),
      updatePostTimestampStatement
        .bind()
        .setString("id", event.postId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updatePostDocName(event: PostEntity.PostDocNameUpdated): Future[Seq[BoundStatement]] =
    execute(
      updatePostDocNameStatement
        .bind()
        .setString("post_id", event.postId)
        .setString("doc_id", event.docId)
        .setString("name", event.name),
      updatePostTimestampStatement
        .bind()
        .setString("id", event.postId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def removePostDoc(event: PostEntity.PostDocRemoved): Future[Seq[BoundStatement]] =
    execute(
      removePostDocStatement
        .bind()
        .setString("post_id", event.postId)
        .setString("doc_id", event.docId),
      updatePostTimestampStatement
        .bind()
        .setString("id", event.postId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def getPostById(id: PostId): Future[Option[Post]] =
    for {
      stmt        <- session.prepare("SELECT * FROM posts WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertPost))
      targets     <- maybeEntity.map(_ => getPostTargets(id)).getOrElse(Future.successful(Set.empty[AnnettePrincipal]))
      media       <- maybeEntity.map(_ => getPostMedia(id)).getOrElse(Future.successful(Map.empty[MediaId, Media]))
      docs        <- maybeEntity.map(_ => getPostDocs(id)).getOrElse(Future.successful(Map.empty[DocId, Doc]))
    } yield maybeEntity.map(
      _.copy(
        targets = targets,
        media = media,
        docs = docs
      )
    )

  private def getPostTargets(id: PostId): Future[Set[AnnettePrincipal]] =
    for {
      stmt   <- session.prepare("SELECT  principal_type, principal_id FROM post_targets WHERE post_id = ?")
      result <- session.selectAll(stmt.bind(id)).map(_.map(convertTargets).toSet)
    } yield result

  private def convertTargets(row: Row): AnnettePrincipal =
    AnnettePrincipal(
      principalType = row.getString("principal_type"),
      principalId = row.getString("principal_id")
    )

  def getPostMedia(id: PostId): Future[Map[MediaId, Media]] =
    for {
      stmt   <- session.prepare("SELECT  media_id, filename FROM post_media WHERE post_id = ?")
      result <- session.selectAll(stmt.bind(id)).map(_.map(convertMedia).map(a => a.id -> a).toMap)
    } yield result

  private def convertMedia(row: Row): Media =
    Media(
      row.getString("media_id"),
      row.getString("filename")
    )

  private def getPostDocs(id: PostId): Future[Map[DocId, Doc]] =
    for {
      stmt   <- session.prepare("SELECT  doc_id, name, filename FROM post_docs WHERE post_id = ?")
      result <- session.selectAll(stmt.bind(id)).map(_.map(convertDoc).map(a => a.id -> a).toMap)
    } yield result

  private def convertDoc(row: Row): Doc =
    Doc(
      row.getString("doc_id"),
      row.getString("name"),
      row.getString("filename")
    )

  def getPostAnnotationById(id: PostId): Future[Option[PostAnnotation]] =
    for {
      stmt        <- session.prepare("""
                                | SELECT id, space_id, featured, author_id_type, author_id_id,
                                |    title, intro_content, publication_status, publication_timestamp,
                                |    updated_at, updated_by_type, updated_by_id
                                |  FROM posts WHERE id = :id
                                |""".stripMargin)
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertPostAnnotation))
    } yield maybeEntity

  def getPostsById(ids: Set[PostId]): Future[Seq[Post]] =
    Source(ids)
      .mapAsync(1)(getPostById)
      .runWith(Sink.seq)
      .map(_.flatten)

  def getPostAnnotationsById(ids: Set[PostId]): Future[Seq[PostAnnotation]] =
    for {
      stmt   <- session.prepare("""
                                | SELECT id, space_id, featured, author_id_type, author_id_id,
                                |    title, intro_content, publication_status, publication_timestamp,
                                |    updated_at, updated_by_type, updated_by_id
                                |  FROM posts WHERE id IN ?
                                |""".stripMargin)
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertPostAnnotation))
    } yield result

  def canAccessToPost(id: PostId, principals: Set[AnnettePrincipal]): Future[Boolean] =
    for {
      stmt  <- session.prepare("SELECT count(*) FROM post_targets WHERE post_id=:id AND principal IN :principals")
      count <- session
                 .selectOne(
                   stmt
                     .bind()
                     .setString("id", id)
                     .setList("principals", principals.map(_.code).toList.asJava)
                 )
                 .map(_.map(_.getLong("count").toInt).getOrElse(0))
    } yield count > 0

  def getPostViewsById(payload: GetPostViewsPayload): Future[Seq[PostView]] = {
    val fields =
      if (payload.withContent)
        "id, space_id, featured, author_id_type, author_id_id, title, intro_content, content, publication_status, publication_timestamp, updated_at, updated_by_type, updated_by_id"
      else
        "id, space_id, featured, author_id_type, author_id_id, title, intro_content, publication_status, publication_timestamp, updated_at, updated_by_type, updated_by_id"

    for {
      allowedPostIds    <- getAllowedPostIds(payload.ids, payload.principals + payload.directPrincipal)
      stmt              <- session.prepare(s" SELECT $fields  FROM posts WHERE id IN ?")
      postViews         <- session
                             .selectAll(stmt.bind(allowedPostIds.toList.asJava))
                             .map(_.map(r => convertPostView(r, payload.withContent)))
      publishedPostViews = postViews.filter(post =>
                             post.publicationStatus == PublicationStatus.Published &&
                               post.publicationTimestamp.map(_.compareTo(OffsetDateTime.now) <= 0).getOrElse(true)
                           )
      metrics           <- getPostMetricsById(publishedPostViews.map(_.id).toSet, payload.directPrincipal)
      metricsMap         = metrics.map(a => a.id -> a).toMap

    } yield publishedPostViews
      .map(pv => pv.copy(metric = metricsMap.get(pv.id)))
  }

  private def getAllowedPostIds(ids: Set[PostId], principals: Set[AnnettePrincipal]): Future[Set[String]] =
    for {
      stmt    <- session.prepare("SELECT post_id FROM post_targets WHERE post_id IN :ids AND principal IN :principals")
      postIds <- session
                   .selectAll(
                     stmt
                       .bind()
                       .setList("ids", ids.toList.asJava)
                       .setList("principals", principals.map(_.code).toList.asJava)
                   )
                   .map(_.map(_.getString("post_id")).toSet)
    } yield postIds

  private def convertPostView(row: Row, withContent: Boolean): PostView = {
    val publicationStatus = convertPublicationStatus(row)
    val content           =
      if (withContent) Some(Json.parse(row.getString("content")).as[PostContent])
      else None
    PostView(
      id = row.getString("id"),
      spaceId = row.getString("space_id"),
      featured = row.getBool("featured"),
      authorId = convertAnnettePrincipal(row, "author_id_type", "author_id_id"),
      title = row.getString("title"),
      introContent = Json.parse(row.getString("intro_content")).as[PostContent],
      content = content,
      publicationStatus = publicationStatus,
      publicationTimestamp = Option(row.getString("publication_timestamp")).map(OffsetDateTime.parse),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = convertAnnettePrincipal(row)
    )
  }

  private def convertAnnettePrincipal(
    row: Row,
    typeField: String = "updated_by_type",
    idField: String = "updated_by_id"
  ) =
    AnnettePrincipal(
      principalType = row.getString(typeField),
      principalId = row.getString(idField)
    )

  private def convertPublicationStatus(row: Row) =
    row.getString("publication_status") match {
      case "published" => PublicationStatus.Published
      case _           => PublicationStatus.Draft
    }

  private def convertPost(row: Row): Post = {
    val publicationStatus = convertPublicationStatus(row)
    Post(
      id = row.getString("id"),
      spaceId = row.getString("space_id"),
      featured = row.getBool("featured"),
      authorId = convertAnnettePrincipal(row, "author_id_type", "author_id_id"),
      title = row.getString("title"),
      introContent = Json.parse(row.getString("intro_content")).as[PostContent],
      content = Json.parse(row.getString("content")).as[PostContent],
      publicationStatus = publicationStatus,
      publicationTimestamp = Option(row.getString("publication_timestamp")).map(OffsetDateTime.parse),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = convertAnnettePrincipal(row)
    )
  }

  private def convertPostAnnotation(row: Row): PostAnnotation = {
    val publicationStatus = convertPublicationStatus(row)
    PostAnnotation(
      id = row.getString("id"),
      spaceId = row.getString("space_id"),
      featured = row.getBool("featured"),
      authorId = convertAnnettePrincipal(row, "author_id_type", "author_id_id"),
      title = row.getString("title"),
      introContent = Json.parse(row.getString("intro_content")).as[PostContent],
      publicationStatus = publicationStatus,
      publicationTimestamp = Option(row.getString("publication_timestamp")).map(OffsetDateTime.parse),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = convertAnnettePrincipal(row)
    )
  }

  // ***************************** metrics update *****************************

  def viewPost(id: PostId, principal: AnnettePrincipal): Future[Done] =
    session.executeWrite(
      viewPostStatement
        .bind()
        .setString("post_id", id)
        .setString("principal", principal.code)
    )

  def likePost(id: PostId, principal: AnnettePrincipal): Future[Done] =
    session.executeWrite(
      likePostStatement
        .bind()
        .setString("post_id", id)
        .setString("principal", principal.code)
    )

  def unlikePost(id: PostId, principal: AnnettePrincipal): Future[Done] =
    session.executeWrite(
      unlikePostStatement
        .bind()
        .setString("post_id", id)
        .setString("principal", principal.code)
    )

  // ***************************** metrics *****************************

  def getPostMetricsById(ids: Set[PostId], principal: AnnettePrincipal): Future[Seq[PostMetric]] =
    Source(ids)
      .mapAsync(1)(id => getPostMetricById(id, principal))
      .runWith(Sink.seq)

  def getPostMetricById(id: PostId, principal: AnnettePrincipal): Future[PostMetric] =
    for {
      views     <- getPostViewsCountById(id)
      likes     <- getPostLikesCountById(id)
      likedByMe <- getPostLikedByMeById(id, principal)
    } yield PostMetric(id, views, likes, likedByMe)

  private def getPostViewsCountById(id: PostId): Future[Int] =
    for {
      viewsStmt <- session.prepare("SELECT count(*) from post_views where post_id =  :post_id")
      views     <-
        session.selectOne(viewsStmt.bind().setString("post_id", id)).map(_.map(_.getLong("count").toInt).getOrElse(0))
    } yield views

  private def getPostLikesCountById(id: PostId): Future[Int] =
    for {
      likesStmt <- session.prepare("SELECT count(*) from post_likes where post_id =  :post_id")
      likes     <-
        session.selectOne(likesStmt.bind().setString("post_id", id)).map(_.map(_.getLong("count").toInt).getOrElse(0))
    } yield likes

  private def getPostLikedByMeById(id: PostId, principal: AnnettePrincipal): Future[Boolean] =
    for {
      likedByMeStmt <-
        session.prepare("SELECT count(*) from post_likes where post_id =  :post_id AND principal = :principal")
      likedByMe     <- session
                         .selectOne(likedByMeStmt.bind().setString("post_id", id).setString("principal", principal.code))
                         .map(_.map(_.getLong("count") > 0).getOrElse(false))
    } yield likedByMe

  private def execute(statements: BoundStatement*): Future[List[BoundStatement]] =
    for (
      _ <- Source(statements)
             .mapAsync(1) { statement =>
               val future = session.executeWrite(statement)
               future.failed.foreach(th => log.error("Failed to process statement {}", statement, th))
               future
             }
             .runWith(Sink.seq)
    ) yield List.empty
  //    Future.successful(statements.toList)
}
