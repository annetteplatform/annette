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

package biz.lobachev.annette.core.elastic

import java.security.cert.X509Certificate

import com.sksamuel.elastic4s.http.{JavaClient, _}
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.ssl.{SSLContexts, TrustStrategy}
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback

object ElasticProvider {

  def create(elasticSettings: ElasticSettings): ElasticClient = {

    val maybeProvider = for {
      username <- elasticSettings.username
      password <- elasticSettings.password
    } yield {
      val provider    = new BasicCredentialsProvider
      val credentials = new UsernamePasswordCredentials(username, password)
      provider.setCredentials(AuthScope.ANY, credentials)
      provider
    }

    val mayBeSslContext =
      if (elasticSettings.allowInsecure)
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
        ElasticProperties(elasticSettings.url),
        NoOpRequestConfigCallback,
        new HttpClientConfigCallback {
          override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder) = {
            var res = httpClientBuilder
            res = maybeProvider.map(provider => res.setDefaultCredentialsProvider(provider)).getOrElse(res)
            res = mayBeSslContext.map { sslContext =>
              res
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            }.getOrElse(res)
            res
          }
        }
      )
    )
  }
}
