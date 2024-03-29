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

package biz.lobachev.annette.cms.impl.files.model

import biz.lobachev.annette.cms.impl.files.FileEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object FileSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[FileEntity],
      JsonSerializer[FileState],
      // responses
      JsonSerializer[FileEntity.Success.type],
      JsonSerializer[FileEntity.SuccessFile],
      JsonSerializer[FileEntity.FileNotFound.type],
      // events
      JsonSerializer[FileEntity.FileStored],
      JsonSerializer[FileEntity.FileRemoved]
    )
}
