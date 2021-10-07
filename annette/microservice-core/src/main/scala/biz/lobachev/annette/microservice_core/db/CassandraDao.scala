package biz.lobachev.annette.microservice_core.db

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait CassandraDao {

  val session: CassandraSession
  implicit val ec: ExecutionContext
  implicit val materializer: Materializer

  protected val log: Logger

  protected def execute(statements: BoundStatement*): Future[Done] =
    for (
      _ <- Source(statements)
             .mapAsync(1) { statement =>
               val future = session.executeWrite(statement)
               future.failed.foreach(th => log.error("Failed to process statement {}", statement, th))
               future
             }
             .runWith(Sink.seq)
    ) yield Done

}
