package biz.lobachev.annette.blogs.impl.post_metric.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.blogs.api.post.PostId
import biz.lobachev.annette.blogs.api.post_metric._
import biz.lobachev.annette.blogs.impl.post_metric.PostMetricEntity
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}

private[impl] class PostMetricCassandraDbDao(session: CassandraSession)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var viewPostStatement: PreparedStatement        = _
  private var likePostStatement: PreparedStatement        = _
  private var deletePostViewsStatement: PreparedStatement = _
  private var deletePostLikesStatement: PreparedStatement = _

  def createTables(): Future[Done] =
    for {
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

  def prepareStatements(): Future[Done] =
    for {
      viewPostStmt        <- session.prepare(
                               """
                          | UPDATE post_views SET
                          |     views = views + 1
                          |   WHERE
                          |      post_id = :post_id AND
                          |      principal = :principal
                          |""".stripMargin
                             )
      likePostStmt        <- session.prepare(
                               """
                          | INSERT INTO post_likes (post_id, principal)
                          |   VALUES (:post_id, :principal)
                          |""".stripMargin
                             )
      deletePostViewsStmt <- session.prepare(
                               """
                                 | DELETE FROM post_views
                                 |   WHERE post_id = :post_id
                                 |""".stripMargin
                             )
      deletePostLikesStmt <- session.prepare(
                               """
                                 | DELETE FROM post_likes
                                 |   WHERE post_id = :post_id
                                 |""".stripMargin
                             )
    } yield {
      viewPostStatement = viewPostStmt
      likePostStatement = likePostStmt
      deletePostViewsStatement = deletePostViewsStmt
      deletePostLikesStatement = deletePostLikesStmt
      Done
    }

  def viewPost(event: PostMetricEntity.PostViewed): Future[Seq[BoundStatement]] =
    build(
      viewPostStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def likePost(event: PostMetricEntity.PostLiked): Future[Seq[BoundStatement]] =
    build(
      likePostStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def deletePostMetric(event: PostMetricEntity.PostMetricDeleted): Future[Seq[BoundStatement]] =
    build(
      deletePostViewsStatement
        .bind()
        .setString("post_id", event.id),
      deletePostLikesStatement
        .bind()
        .setString("post_id", event.id)
    )

  def getPostMetricById(id: PostId): Future[Option[PostMetric]] =
    for {
      // TODO: change the following code
      viewsStmt <- session.prepare("SELECT count(*) from post_views where post_id =  :post_id")
      views     <-
        session.selectOne(viewsStmt.bind().setString("post_id", id)).map(_.map(_.getLong("count").toInt).getOrElse(0))
      likesStmt <- session.prepare("SELECT count(*) from post_likes where post_id =  :post_id")
      likes     <-
        session.selectOne(likesStmt.bind().setString("post_id", id)).map(_.map(_.getLong("count").toInt).getOrElse(0))

    } yield Some(PostMetric(id, views, likes))

  def getPostMetricsById(ids: Set[PostId]): Future[Map[PostId, PostMetric]]    =
    Source(ids)
      .mapAsync(1)(getPostMetricById)
      .runWith(Sink.seq)
      .map(_.flatten.map(a => a.id -> a).toMap)

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)

}
