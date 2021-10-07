package biz.lobachev.annette.microservice_core.db

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.{CassandraLagomAsyncContext, SnakeCase}

trait CassandraQuillDao extends QuillEncoders {

  val session: CassandraSession

  lazy val ctx = new CassandraLagomAsyncContext(SnakeCase, session)

}
