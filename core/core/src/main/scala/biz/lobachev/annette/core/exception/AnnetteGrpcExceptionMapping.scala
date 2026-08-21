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

package biz.lobachev.annette.core.exception

import io.grpc.{Metadata, Status, StatusRuntimeException}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.grpc.scaladsl.GrpcExceptionHandler
import org.apache.pekko.grpc.Trailers
import org.apache.pekko.grpc.internal.GrpcMetadataImpl
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Encodes/decodes [[AnnetteTransportException]] across Pekko gRPC (design 001-decisions.md §D).
 *
 * Server side: wire `AnnetteGrpcExceptionMapping.serverHandler` as the generated handler's
 * `eHandler` — a failed RPC is completed with a gRPC status derived from the exception's HTTP
 * code, and the full error payload (code + params + http status) travels in the `annette-error-bin`
 * (binary, UTF-8 JSON) trailer. Non-annette failures fall through to the default handler.
 *
 * Client side: wrap generated `*ServiceClient` calls with [[recoverAnnette]] to rehydrate the
 * exception, so gateway error mapping and client-side `recover { case SomeDomainError(...) }`
 * composition keep working exactly as they did over Lagom's transport.
 */
object AnnetteGrpcExceptionMapping {

  val TrailerKey: Metadata.Key[Array[Byte]] =
    Metadata.Key.of("annette-error-bin", Metadata.BINARY_BYTE_MARSHALLER)

  private def grpcCode(httpCode: Int): Status.Code =
    httpCode match {
      case 400 => Status.Code.INVALID_ARGUMENT
      case 403 => Status.Code.PERMISSION_DENIED
      case 404 => Status.Code.NOT_FOUND
      case _   => Status.Code.INTERNAL
    }

  private def encode(ex: AnnetteTransportException): Array[Byte] =
    Json
      .obj(
        "code"   -> ex.code,
        "params" -> Json.toJson(ex.params),
        "http"   -> ex.errorCode.http
      )
      .toString
      .getBytes(StandardCharsets.UTF_8)

  private def decode(bytes: Array[Byte]): Option[AnnetteTransportException] =
    Try {
      val json = Json.parse(new String(bytes, StandardCharsets.UTF_8))
      val code = (json \ "code").as[String]
      val params = (json \ "params").as[Map[String, String]]
      val http = (json \ "http").as[Int]
      AnnetteTransportException(canonicalErrorCode(http), code, params)
    }.toOption

  /**
   * Map the wire HTTP status back to the canonical [[TransportErrorCode]] constant. Typed
   * client-side matching (`case XxxAlreadyExist(_)` via `AnnetteTransportExceptionCompanion`
   * `unapply`) compares errorCode for equality with e.g. `TransportErrorCode.BadRequest` —
   * rebuilding a fresh `TransportErrorCode(400, "HTTP 400")` would break every such match
   * (first observed at runtime with demo-ignition upserts in slice 013).
   */
  private def canonicalErrorCode(http: Int): TransportErrorCode =
    http match {
      case 400 => TransportErrorCode.BadRequest
      case 403 => TransportErrorCode.Forbidden
      case 404 => TransportErrorCode.NotFound
      case _   => TransportErrorCode.InternalServerError
    }

  /** Server-side `eHandler` (orElse'd with the pekko-grpc default by the generated handler). */
  def serverHandler(system: ActorSystem): PartialFunction[Throwable, Trailers] = {
    case ex: AnnetteTransportException =>
      system.log.debug("gRPC call failed with annette exception [{}]", ex.code)
      val metadata = new Metadata()
      metadata.put(TrailerKey, encode(ex))
      Trailers(grpcCode(ex.errorCode.http).toStatus.withDescription(ex.code), new GrpcMetadataImpl(metadata))
  }

  /** Convenience for callers that need the full chain explicitly. */
  def serverHandlerOrDefault(system: ActorSystem): PartialFunction[Throwable, Trailers] =
    serverHandler(system).orElse(GrpcExceptionHandler.defaultMapper(system))

  /**
   * Client-side recovery: rehydrates [[AnnetteTransportException]] from a failed generated-client
   * call. Unannotated failures (including plain transport errors) pass through unchanged.
   */
  def recoverAnnette[T](f: Future[T])(implicit ec: ExecutionContext): Future[T] =
    f.recoverWith { case th => Future.failed(recover(th)) }

  private def recover(th: Throwable): Throwable = th match {
    case ex: java.util.concurrent.ExecutionException if ex.getCause != null => recover(ex.getCause)
    case sre: StatusRuntimeException =>
      val bytes = if (sre.getTrailers == null) null else sre.getTrailers.get(TrailerKey)
      if (bytes == null) sre else decode(bytes).getOrElse(sre)
    case other => other
  }
}
