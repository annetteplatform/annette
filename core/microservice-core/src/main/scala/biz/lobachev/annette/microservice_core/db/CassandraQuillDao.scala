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

package biz.lobachev.annette.microservice_core.db

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.{CassandraLagomAsyncContext, SnakeCase}

trait CassandraQuillDao extends QuillEncoders {

  val session: CassandraSession

  lazy val ctx: CassandraLagomAsyncContext[SnakeCase.type] = new CassandraLagomAsyncContext(SnakeCase, session)

  @annotation.nowarn
  def touch(obj: Any) = ()

}
