package biz.lobachev.annette.blogs.impl.post_metric.dao

import akka.Done
import biz.lobachev.annette.blogs.api.post.PostId
import biz.lobachev.annette.blogs.api.post_metric._
import biz.lobachev.annette.blogs.impl.post_metric.PostMetricEntity
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class PostMetricCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var viewPostStatement: PreparedStatement         = _
  private var likePostStatement: PreparedStatement         = _
  private var deletePostMetricStatement: PreparedStatement = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS ?????? (
                                        |          id               text PRIMARY KEY,
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      viewPostStmt         <- session.prepare(
                                """
                          |
                          |""".stripMargin
                              )
      likePostStmt         <- session.prepare(
                                """
                          |
                          |""".stripMargin
                              )
      deletePostMetricStmt <- session.prepare(
                                """
                                  |
                                  |""".stripMargin
                              )
    } yield {
      viewPostStatement = viewPostStmt
      likePostStatement = likePostStmt
      deletePostMetricStatement = deletePostMetricStmt
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
      deletePostMetricStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def getPostMetricById(id: PostId): Future[Option[PostMetric]] =
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM ??? WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertPostMetric))
    } yield maybeEntity

  def getPostMetricsById(ids: Set[PostId]): Future[Map[PostId, PostMetric]] =
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM ??? WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertPostMetric))
    } yield result.map(a => a.id -> a).toMap

  private def convertPostMetric(row: Row): PostMetric                       = ???

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)

}
