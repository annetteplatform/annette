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

package biz.lobachev.annette.persons.impl.person.model
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.PersonEntity.{
  AlreadyExist,
  Confirmation,
  NotFound,
  PersonCreated,
  PersonDeleted,
  PersonUpdated,
  Success,
  SuccessPerson
}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object PersonSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[Person],
      JsonSerializer[PersonState],
      JsonSerializer[CreatePersonPayload],
      JsonSerializer[UpdatePersonPayload],
      JsonSerializer[DeletePersonPayload],
      // responses
      JsonSerializer[Confirmation],
      JsonSerializer[Success.type],
      JsonSerializer[SuccessPerson],
      JsonSerializer[NotFound.type],
      JsonSerializer[AlreadyExist.type],
      // events
      JsonSerializer[PersonCreated],
      JsonSerializer[PersonUpdated],
      JsonSerializer[PersonDeleted],
      JsonSerializer[PersonFindQuery],
      JsonSerializer[FindResult]
    )
}
