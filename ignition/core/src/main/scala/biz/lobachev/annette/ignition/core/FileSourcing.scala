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

package biz.lobachev.annette.ignition.core

import org.slf4j.Logger
import play.api.libs.json.{Json, Reads}

import scala.util.{Failure, Success, Try}
import scala.io.Source

protected trait FileSourcing {
  protected val log: Logger

  protected def getData[A](name: String, filename: String)(implicit reads: Reads[A]): Either[Throwable, A] = {
    val jsonTry = Try(Source.fromResource(filename).mkString)
    jsonTry match {
      case Success(json) =>
        val resTry = Try(Json.parse(json).as[A])
        resTry match {
          case Success(seq) => Right(seq)
          case Failure(th)  =>
            val message = s"Parsing $name json failed: $filename"
            log.error(message, th)
            Left(new IllegalArgumentException(message, th))
        }
      case Failure(th)   =>
        val message = s"$name file load failed: $filename"
        log.error(message, th)
        Left(new IllegalArgumentException(message, th))

    }
  }
}
