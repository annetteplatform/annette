package biz.lobachev.annette.persons.impl

import biz.lobachev.annette.persons.impl.category.model.CategorySerializerRegistry
import biz.lobachev.annette.persons.impl.person.model.PersonSerializerRegistry
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import scala.collection.immutable

object PersonRepositorySerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    PersonSerializerRegistry.serializers ++ CategorySerializerRegistry.serializers
}
