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

package biz.lobachev.annette.bpm_repository.api.rdb

object SQLErrorCodes {
  // https://www.postgresql.org/docs/9.4/errcodes-appendix.html
  final val STRING_DATA_RIGHT_TRUNCATION = "22001"
  final val FOREIGN_KEY_VIOLATION        = "23503"
  final val UNIQUE_VIOLATION             = "23505"
}
