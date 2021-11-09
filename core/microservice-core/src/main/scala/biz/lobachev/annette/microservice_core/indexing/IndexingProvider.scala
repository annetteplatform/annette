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

package biz.lobachev.annette.microservice_core.indexing

import biz.lobachev.annette.microservice_core.indexing.config.IndexConfig
import com.sksamuel.elastic4s.http._
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.ssl.{SSLContexts, TrustStrategy}
import pureconfig._
import pureconfig.generic.auto._

import java.security.cert.X509Certificate

object IndexingProvider {

  def loadIndexConfig(configPath: String): IndexConfig =
    ConfigSource.default
      .at(configPath)
      .load[IndexConfig]
      .fold(
        failure => {
          val description = failure.toList.map(_.description).mkString(", ")
          throw IndexConfigError(configPath, description)
        },
        config => {
          val fields = config.mappings.map { case k -> v => v.field.getOrElse(k) }
            .groupMapReduce(k => k)(_ => 1) { case (v, acc) => acc + v }
            .map { case field -> count => if (count > 1) Some(field) else None }
            .flatten
            .mkString(", ")
          if (fields.isBlank)
            config
          else
            throw DuplicateIndexFields(fields)

        }
      )

  def loadConnectionConfig(configPath: ConnectionConfigPath): ConnectionConfig =
    ConfigSource.default
      .at(configPath.path)
      .load[ConnectionConfig]
      .fold(
        failure => {
          val description = failure.toList.map(_.toString).mkString(", ")
          throw ConnectionConfigError(configPath.path, description)
        },
        config => config
      )

  def createClient(connectionConfig: ConnectionConfig): ElasticClient = {
    val maybeProvider = for {
      username <- connectionConfig.username
      password <- connectionConfig.password
    } yield {
      val provider    = new BasicCredentialsProvider
      val credentials = new UsernamePasswordCredentials(username, password)
      provider.setCredentials(AuthScope.ANY, credentials)
      provider
    }

    val mayBeSslContext =
      if (connectionConfig.allowInsecure)
        Some(
          SSLContexts
            .custom()
            .loadTrustMaterial(new TrustStrategy() {
              def isTrusted(chain: Array[X509Certificate], authType: String): Boolean = true
            })
            .build
        )
      else None

    ElasticClient(
      JavaClient(
        ElasticProperties(connectionConfig.url),
        NoOpRequestConfigCallback,
        (httpClientBuilder: HttpAsyncClientBuilder) => {
          var res = httpClientBuilder
          res = maybeProvider.map(provider => res.setDefaultCredentialsProvider(provider)).getOrElse(res)
          res = mayBeSslContext.map { sslContext =>
            res
              .setSSLContext(sslContext)
              .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
          }.getOrElse(res)
          res
        }
      )
    )
  }
}
